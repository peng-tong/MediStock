package com.medistock.purchase.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.medistock.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 采购单明细 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("purchase_order_item")
public class PurchaseOrderItem extends BaseEntity {
    private Long id;
    private Long tenantId;
    private Long orderId;
    private Long drugId;
    private BigDecimal quantity;
    private BigDecimal purchasePrice;
    private BigDecimal amount;
    private BigDecimal inboundQuantity;   // 已入库数量（P1 多次入库口子）
    private String batchNo;               // 入库时回填
    private LocalDate expiryDate;         // 入库时回填

    /** 展示字段：药品通用名 / 规格 / 存储条件（详情接口按 drugId 回填，不落库） */
    @TableField(exist = false)
    private String drugName;
    @TableField(exist = false)
    private String spec;
    @TableField(exist = false)
    private String storageCondition;
}
