package com.medistock.purchase.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.medistock.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/** 采购退货单头 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("purchase_return")
public class PurchaseReturn extends BaseEntity {
    private Long id;
    private Long tenantId;
    private String returnNo;
    private Long orderId;        // 关联采购单（可为空，支持直接退货）
    private Long supplierId;
    private Long warehouseId;
    private BigDecimal totalAmount;
    /** 0草稿 1已完成 2已取消 */
    private Integer status;
    private String remark;
    /** 展示字段：供应商名称 / 经办人姓名（非表字段） */
    @TableField(exist = false)
    private String supplierName;
    @TableField(exist = false)
    private String createByName;
}
