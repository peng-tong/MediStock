package com.medistock.purchase.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.medistock.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/** 采购退货明细 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("purchase_return_item")
public class PurchaseReturnItem extends BaseEntity {
    private Long id;
    private Long tenantId;
    private Long returnId;
    private Long drugId;
    private Long batchId;        // 退货批次（从哪个批次退）
    private BigDecimal quantity;
    private BigDecimal returnPrice;
    private BigDecimal amount;
}
