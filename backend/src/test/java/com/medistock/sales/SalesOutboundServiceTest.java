package com.medistock.sales;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medistock.common.config.TenantContext;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.inventory.entity.StockBatch;
import com.medistock.inventory.mapper.StockBatchMapper;
import com.medistock.inventory.service.StockService;
import com.medistock.sales.entity.SalesOrder;
import com.medistock.sales.entity.SalesOrderItem;
import com.medistock.sales.service.SalesOrderService;
import com.medistock.sales.service.SalesOutboundService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class SalesOutboundServiceTest {

    @Autowired private SalesOrderService orderService;
    @Autowired private SalesOutboundService outboundService;
    @Autowired private StockService stockService;
    @Autowired private StockBatchMapper batchMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final Long DRUG = 9301L;
    private static final Long WH = 9302L;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(1L);
        // 物理删除：避免测试重跑时唯一索引 (tenant_id,drug_id,warehouse_id[,batch_no]) 冲突、
        // 流水数据堆积与销售单单号序号递增
        jdbcTemplate.update("DELETE FROM inv_stock_batch WHERE drug_id = ?", DRUG);
        jdbcTemplate.update("DELETE FROM inv_stock WHERE drug_id = ?", DRUG);
        jdbcTemplate.update("DELETE FROM inv_flow WHERE drug_id = ?", DRUG);
        jdbcTemplate.update("DELETE FROM sales_order_item");
        jdbcTemplate.update("DELETE FROM sales_order");
    }

    @Test
    void fifo_deductsEarliestExpiryFirst() {
        TenantContext.setTenantId(1L);
        try {
            // 两个批次：早过期(30天) 10 件，晚过期(300天) 5 件
            stockService.inbound(DRUG, WH, "B-EARLY", LocalDate.now().plusDays(30), new BigDecimal("10"),
                    "PO-1", "早批次");
            stockService.inbound(DRUG, WH, "B-LATE", LocalDate.now().plusDays(300), new BigDecimal("5"),
                    "PO-2", "晚批次");

            // 出库 12：应优先扣早批次（10），再扣晚批次（2）
            SalesOrder order = new SalesOrder();
            order.setCustomerId(1L);
            order.setWarehouseId(WH);
            order.setRemark("FIFO 测试");
            SalesOrderItem item = new SalesOrderItem();
            item.setDrugId(DRUG);
            item.setQuantity(new BigDecimal("12"));
            item.setSalePrice(new BigDecimal("10"));
            item.setAmount(new BigDecimal("120"));
            Long orderId = orderService.createOrder(order, List.of(item));
            // 出库前走状态流转：提交（生产代码要求仅待出库 status=1 的销售单可出库）
            orderService.submitOrder(orderId);

            outboundService.outbound(orderId);

            StockBatch early = batchMapper.selectOne(new LambdaQueryWrapper<StockBatch>()
                    .eq(StockBatch::getBatchNo, "B-EARLY"));
            StockBatch late = batchMapper.selectOne(new LambdaQueryWrapper<StockBatch>()
                    .eq(StockBatch::getBatchNo, "B-LATE"));
            assertThat(early.getQuantity()).isEqualByComparingTo("0");   // 早批次先耗尽
            assertThat(late.getQuantity()).isEqualByComparingTo("3");    // 晚批次只扣 2
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void expiredBatch_isRejected() {
        TenantContext.setTenantId(1L);
        try {
            // 只有过期批次：出库应报"库存不足"（过期批次不可用）
            stockService.inbound(DRUG, WH, "B-EXP", LocalDate.now().minusDays(1), new BigDecimal("10"),
                    "PO-3", "过期批次");
            SalesOrder order = new SalesOrder();
            order.setCustomerId(1L);
            order.setWarehouseId(WH);
            SalesOrderItem item = new SalesOrderItem();
            item.setDrugId(DRUG);
            item.setQuantity(new BigDecimal("1"));
            item.setSalePrice(new BigDecimal("10"));
            item.setAmount(new BigDecimal("10"));
            Long orderId = orderService.createOrder(order, List.of(item));
            orderService.submitOrder(orderId);

            // 断言具体错误码：过期批次不可出库 → 报 STOCK_INSUFFICIENT（负库存/无可用批次拦截）
            assertThatThrownBy(() -> outboundService.outbound(orderId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ErrorCode.STOCK_INSUFFICIENT.getCode());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void concurrentOutbound_noOversell() throws Exception {
        TenantContext.setTenantId(1L);
        try {
            stockService.inbound(DRUG, WH, "B-CONC", LocalDate.now().plusDays(90), new BigDecimal("100"),
                    "PO-4", "并发测试");
            // 10 张单并发出库，每张 10 → 全部成功，库存归 0
            int threads = 10;
            AtomicInteger success = new AtomicInteger();
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        // 线程池线程不继承主线程 ThreadLocal，需手动注入租户上下文
                        // （否则租户拦截器放行、INSERT 流水时 tenant_id 为 NULL 违反 NOT NULL 约束）
                        TenantContext.setTenantId(1L);
                        SalesOrder o = new SalesOrder();
                        o.setCustomerId(1L);
                        o.setWarehouseId(WH);
                        SalesOrderItem it = new SalesOrderItem();
                        it.setDrugId(DRUG);
                        it.setQuantity(new BigDecimal("10"));
                        it.setSalePrice(new BigDecimal("10"));
                        it.setAmount(new BigDecimal("100"));
                        Long id = orderService.createOrder(o, List.of(it));
                        orderService.submitOrder(id);
                        outboundService.outbound(id);
                        success.incrementAndGet();
                    } catch (Exception ignored) {
                        // 预期失败兜底（含 start.await() 的 InterruptedException）；库存恰好充足场景应全部成功
                    } finally {
                        TenantContext.clear();
                        done.countDown();
                    }
                });
            }
            ready.await();
            start.countDown();
            done.await();
            pool.shutdown();

            assertThat(success.get()).isEqualTo(threads);
            StockBatch batch = batchMapper.selectOne(new LambdaQueryWrapper<StockBatch>()
                    .eq(StockBatch::getBatchNo, "B-CONC"));
            assertThat(batch.getQuantity()).isEqualByComparingTo("0");   // 不超卖
        } finally {
            TenantContext.clear();
        }
    }
}
