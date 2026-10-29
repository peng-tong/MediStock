package com.medistock.purchase;

import com.medistock.common.config.TenantContext;
import com.medistock.purchase.entity.PurchaseOrder;
import com.medistock.purchase.entity.PurchaseOrderItem;
import com.medistock.purchase.service.PurchaseOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PurchaseOrderServiceTest {

    @Autowired private PurchaseOrderService orderService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(1L);
        // 物理删除：MP 逻辑删除（deleted=1）后行仍占据记录，测试重跑会数据堆积（单号序号递增），
        // 故用 JdbcTemplate 直接 DELETE 清理采购单及其明细
        jdbcTemplate.update("DELETE FROM purchase_order_item");
        jdbcTemplate.update("DELETE FROM purchase_order");
    }

    @Test
    void createOrder_setsStatusAndTotalAmount() {
        TenantContext.setTenantId(1L);
        try {
            PurchaseOrder order = new PurchaseOrder();
            order.setSupplierId(1L);
            order.setWarehouseId(1L);
            order.setRemark("测试采购单");
            PurchaseOrderItem item1 = new PurchaseOrderItem();
            item1.setDrugId(1L);
            item1.setQuantity(new BigDecimal("10"));
            item1.setPurchasePrice(new BigDecimal("5"));
            PurchaseOrderItem item2 = new PurchaseOrderItem();
            item2.setDrugId(2L);
            item2.setQuantity(new BigDecimal("4"));
            item2.setPurchasePrice(new BigDecimal("2.5"));
            Long orderId = orderService.createOrder(order, List.of(item1, item2));

            // 创建后 status=0（草稿），总金额 = 明细合计（10*5 + 4*2.5 = 60），单号 PO+日期+序号
            PurchaseOrder saved = orderService.getById(orderId);
            assertThat(saved.getStatus()).isEqualTo(0);
            assertThat(saved.getTotalAmount()).isEqualByComparingTo("60");
            assertThat(saved.getOrderNo()).startsWith("PO" + java.time.LocalDate.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")));

            // 明细行金额 = 单价 × 数量，行数正确
            List<PurchaseOrderItem> items = orderService.listItems(orderId);
            assertThat(items).hasSize(2);
            assertThat(items.get(0).getAmount()).isEqualByComparingTo("50");
            assertThat(items.get(1).getAmount()).isEqualByComparingTo("10");
            assertThat(items.get(0).getInboundQuantity()).isEqualByComparingTo("0");
        } finally {
            TenantContext.clear();
        }
    }
}
