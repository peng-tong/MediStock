package com.medistock.sales;

import com.medistock.common.config.TenantContext;
import com.medistock.sales.entity.SalesOrder;
import com.medistock.sales.entity.SalesOrderItem;
import com.medistock.sales.service.SalesOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SalesOrderServiceTest {

    @Autowired private SalesOrderService orderService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(1L);
        // 物理删除：MP 逻辑删除（deleted=1）后行仍占据记录，测试重跑会数据堆积（单号序号递增），
        // 故用 JdbcTemplate 直接 DELETE 清理销售单及其明细
        jdbcTemplate.update("DELETE FROM sales_order_item");
        jdbcTemplate.update("DELETE FROM sales_order");
    }

    @Test
    void createOrder_setsStatusAndTotalAmount() {
        TenantContext.setTenantId(1L);
        try {
            SalesOrder order = new SalesOrder();
            order.setCustomerId(1L);
            order.setWarehouseId(1L);
            order.setRemark("测试销售单");
            SalesOrderItem item1 = new SalesOrderItem();
            item1.setDrugId(1L);
            item1.setQuantity(new BigDecimal("10"));
            item1.setSalePrice(new BigDecimal("5"));
            item1.setDiscountAmount(new BigDecimal("5"));   // 行折扣
            SalesOrderItem item2 = new SalesOrderItem();
            item2.setDrugId(2L);
            item2.setQuantity(new BigDecimal("4"));
            item2.setSalePrice(new BigDecimal("2.5"));
            Long orderId = orderService.createOrder(order, List.of(item1, item2));

            // 创建后 status=0（草稿），总金额 = 行金额合计（10*5-5 + 4*2.5 = 55），单号 SO+日期+序号
            SalesOrder saved = orderService.getById(orderId);
            assertThat(saved.getStatus()).isEqualTo(0);
            assertThat(saved.getTotalAmount()).isEqualByComparingTo("55");
            assertThat(saved.getOrderNo()).startsWith("SO" + LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd")));

            // 明细行金额 = 单价×数量 − 行折扣，行数正确
            List<SalesOrderItem> items = orderService.listItems(orderId);
            assertThat(items).hasSize(2);
            assertThat(items.get(0).getAmount()).isEqualByComparingTo("45");
            assertThat(items.get(1).getAmount()).isEqualByComparingTo("10");
            assertThat(items.get(0).getOutboundQuantity()).isEqualByComparingTo("0");
        } finally {
            TenantContext.clear();
        }
    }
}
