package com.medistock.sales.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.medistock.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/** 销售单明细 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sales_order_item")
public class SalesOrderItem extends BaseEntity {
    private Long id;
    private Long tenantId;
    private Long orderId;
    private Long drugId;
    private BigDecimal quantity;
    private BigDecimal salePrice;
    /** 行折扣金额 */
    private BigDecimal discountAmount;
    /** 行实收金额（数量 × 单价 − 折扣） */
    private BigDecimal amount;
    /** 已出库数量（P1 多次出库口子） */
    private BigDecimal outboundQuantity;
}
