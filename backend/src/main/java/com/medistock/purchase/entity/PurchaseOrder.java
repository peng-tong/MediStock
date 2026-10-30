package com.medistock.purchase.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.medistock.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 采购单头 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("purchase_order")
public class PurchaseOrder extends BaseEntity {
    private Long id;
    private Long tenantId;
    private String orderNo;
    private Long supplierId;
    private Long warehouseId;
    private BigDecimal totalAmount;
    /** 0草稿 1待审核(P1) 2已审核(P1) 3已入库 4已取消 */
    private Integer status;
    private Long createBy;
    private Long auditBy;       // P1
    private LocalDateTime auditTime;  // P1
    private String remark;
    /** 明细（非表字段，详情返回） */
    @TableField(exist = false)
    private List<PurchaseOrderItem> items;
    /** 展示字段：供应商名称 / 药品明细摘要 / 明细总数量 / 经办人姓名（列表批量回填） */
    @TableField(exist = false)
    private String supplierName;
    @TableField(exist = false)
    private String drugNames;
    @TableField(exist = false)
    private BigDecimal quantity;
    @TableField(exist = false)
    private String createByName;
}
