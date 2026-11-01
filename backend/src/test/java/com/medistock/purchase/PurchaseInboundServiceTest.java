package com.medistock.purchase;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medistock.common.config.TenantContext;
import com.medistock.inventory.entity.Stock;
import com.medistock.inventory.entity.StockBatch;
import com.medistock.inventory.mapper.StockBatchMapper;
import com.medistock.inventory.mapper.StockMapper;
import com.medistock.purchase.entity.PurchaseOrder;
import com.medistock.purchase.entity.PurchaseOrderItem;
import com.medistock.purchase.service.InboundReq;
import com.medistock.purchase.service.PurchaseInboundService;
import com.medistock.purchase.service.PurchaseOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PurchaseInboundServiceTest {

    @Autowired private PurchaseOrderService orderService;
    @Autowired private PurchaseInboundService inboundService;
    @Autowired private StockMapper stockMapper;
    @Autowired private StockBatchMapper batchMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    /** 入库测试专用数据（与并发测试 9001/9002、盘点 9101/9102 区分开） */
    private static final Long DRUG_ID = 9201L;
    private static final Long WH_ID = 9202L;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(1L);
        // 物理删除：MP 逻辑删除（deleted=1）后行仍占据唯一索引，重跑测试会撞唯一键，故用 JdbcTemplate 直接 DELETE
        jdbcTemplate.update("DELETE FROM inv_stock_batch WHERE drug_id = ?", DRUG_ID);
        jdbcTemplate.update("DELETE FROM inv_stock WHERE drug_id = ?", DRUG_ID);
        jdbcTemplate.update("DELETE FROM inv_flow WHERE drug_id = ?", DRUG_ID);
        // 清理历史采购单及其明细（按仓库维度），避免数据堆积与单号序号递增
        jdbcTemplate.update("DELETE FROM purchase_order_item WHERE order_id IN (SELECT id FROM purchase_order WHERE warehouse_id = ?)", WH_ID);
        jdbcTemplate.update("DELETE FROM purchase_order WHERE warehouse_id = ?", WH_ID);
    }

    @Test
    void inbound_createsBatchAndStock() {
        TenantContext.setTenantId(1L);
        try {
            PurchaseOrder order = new PurchaseOrder();
            order.setSupplierId(1L);
            order.setWarehouseId(WH_ID);
            order.setRemark("测试");
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setDrugId(DRUG_ID);
            item.setQuantity(new BigDecimal("30"));
            item.setPurchasePrice(new BigDecimal("5"));
            Long orderId = orderService.createOrder(order, List.of(item));
            // 入库前走状态流转：提交 → 审核通过（生产代码要求仅审核通过的采购单可入库）
            orderService.submitOrder(orderId);
            orderService.approveOrder(orderId);

            PurchaseOrderItem savedItem = orderService.listItems(orderId).get(0);
            InboundReq.Item in = new InboundReq.Item();
            in.setItemId(savedItem.getId());
            in.setQuantity(new BigDecimal("30"));
            in.setBatchNo("B-IN-1");
            in.setExpiryDate(LocalDate.now().plusDays(365));
            inboundService.inbound(orderId, List.of(in));

            // 批次 + 汇总库存 + 状态
            StockBatch batch = batchMapper.selectOne(new LambdaQueryWrapper<StockBatch>()
                    .eq(StockBatch::getDrugId, DRUG_ID).eq(StockBatch::getBatchNo, "B-IN-1"));
            assertThat(batch.getQuantity()).isEqualByComparingTo("30");
            Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getDrugId, DRUG_ID).eq(Stock::getWarehouseId, WH_ID));
            assertThat(stock.getQuantity()).isEqualByComparingTo("30");
            assertThat(orderService.getById(orderId).getStatus()).isEqualTo(3);
        } finally {
            TenantContext.clear();
        }
    }
}
