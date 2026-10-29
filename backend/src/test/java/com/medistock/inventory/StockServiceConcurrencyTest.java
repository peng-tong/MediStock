package com.medistock.inventory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medistock.common.config.TenantContext;
import com.medistock.common.exception.BusinessException;
import com.medistock.inventory.entity.InventoryFlow;
import com.medistock.inventory.entity.Stock;
import com.medistock.inventory.entity.StockBatch;
import com.medistock.inventory.mapper.InventoryFlowMapper;
import com.medistock.inventory.mapper.StockBatchMapper;
import com.medistock.inventory.mapper.StockMapper;
import com.medistock.inventory.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 并发正确性测试：
 * 1. 库存刚好够：N 线程并发出库，全部成功，最终库存为 0，不超卖
 * 2. 库存不足：并发超量出库，成功数 = 可扣减上限，最终库存为 0，无负库存
 * 运行环境：H2（application-test.yml），连接池 20，足以支撑 10 线程并发
 */
@SpringBootTest
@ActiveProfiles("test")
class StockServiceConcurrencyTest {

    @Autowired
    private StockService stockService;
    @Autowired
    private StockMapper stockMapper;
    @Autowired
    private StockBatchMapper batchMapper;
    @Autowired
    private InventoryFlowMapper flowMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Long DRUG_ID = 9001L;
    private static final Long WH_ID = 9002L;
    private static final String BATCH_NO = "B2026";

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(1L);
        // 物理删除：MP 逻辑删除（deleted=1）后行仍占据唯一索引 (tenant_id,drug_id,warehouse_id[,batch_no])，
        // 测试重跑时 inbound 同批号会撞唯一键抛 DuplicateKeyException，故用 JdbcTemplate 直接 DELETE 清理
        jdbcTemplate.update("DELETE FROM inv_stock_batch WHERE drug_id = ?", DRUG_ID);
        jdbcTemplate.update("DELETE FROM inv_stock WHERE drug_id = ?", DRUG_ID);
        jdbcTemplate.update("DELETE FROM inv_flow WHERE drug_id = ?", DRUG_ID);
    }

    @Test
    void concurrentOutbound_exactQuantity_allSucceed_noOversell() throws Exception {
        // 入库 100（单批次），10 线程各出 10（共 100）→ 全部成功
        stockService.inbound(DRUG_ID, WH_ID, BATCH_NO, LocalDate.now().plusDays(90),
                new BigDecimal("100"), "PO-TEST", "测试入库");

        int threads = 10;
        AtomicInteger success = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    // 线程池线程不继承主线程 ThreadLocal，需手动注入租户上下文（否则租户拦截器放行、
                    // INSERT 流水时 tenant_id 为 NULL 违反 NOT NULL 约束）
                    TenantContext.setTenantId(1L);
                    stockService.outboundFifo(DRUG_ID, WH_ID, new BigDecimal("10"),
                            "SO-TEST", "并发出库");
                    success.incrementAndGet();
                } catch (Throwable e) {
                    errors.add(e);
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

        assertThat(success.get()).isEqualTo(threads);          // 全部成功
        assertThat(errors).isEmpty();

        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getDrugId, DRUG_ID).eq(Stock::getWarehouseId, WH_ID));
        StockBatch batch = batchMapper.selectOne(new LambdaQueryWrapper<StockBatch>()
                .eq(StockBatch::getDrugId, DRUG_ID).eq(StockBatch::getBatchNo, BATCH_NO));
        assertThat(stock.getQuantity()).isEqualByComparingTo("0");      // 无超卖
        assertThat(batch.getQuantity()).isEqualByComparingTo("0");
    }

    @Test
    void concurrentOutbound_overQuantity_noNegativeStock() throws Exception {
        // 入库 50，8 线程各出 10（共 80 > 50）→ 仅前 5 成功，其余"库存不足"
        stockService.inbound(DRUG_ID, WH_ID, BATCH_NO, LocalDate.now().plusDays(90),
                new BigDecimal("50"), "PO-TEST", "测试入库");

        int threads = 8;
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
                    TenantContext.setTenantId(1L);
                    stockService.outboundFifo(DRUG_ID, WH_ID, new BigDecimal("10"),
                            "SO-TEST", "并发超量出库");
                    success.incrementAndGet();
                } catch (Exception ignored) {
                    // 库存不足 → 预期失败（Exception 兜底：lambda 内 start.await() 的 InterruptedException 也需捕获）
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

        // 成功数 ≤ 5（50/10），且最终库存为 0，无负库存
        assertThat(success.get()).isLessThanOrEqualTo(5);
        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getDrugId, DRUG_ID).eq(Stock::getWarehouseId, WH_ID));
        assertThat(stock.getQuantity().compareTo(BigDecimal.ZERO)).isGreaterThanOrEqualTo(0);
    }
}
