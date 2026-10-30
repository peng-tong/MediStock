package com.medistock.sales.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.medistock.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/** 销售单头 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sales_order")
public class SalesOrder extends BaseEntity {
    private Long id;
    private Long tenantId;
    private String orderNo;
    private Long customerId;
    private Long warehouseId;
    private BigDecimal totalAmount;
    /** 整单折扣金额 */
    private BigDecimal discountAmount;
    /** 实收金额 */
    private BigDecimal paidAmount;
    /** 0草稿 1待审核(P1) 2已审核(P1) 3已出库 4已取消 */
    private Integer status;
    private Long createBy;
    private String remark;
    /** 明细（非表字段，详情返回） */
    @TableField(exist = false)
    private List<SalesOrderItem> items;
    /** 展示字段：客户名称 / 开单人姓名（列表批量回填） */
    @TableField(exist = false)
    private String customerName;
    @TableField(exist = false)
    private String createByName;
    /** 展示字段：药品明细摘要（"药品 ×数量、…"）与明细总数量 */
    @TableField(exist = false)
    private String drugNames;
    @TableField(exist = false)
    private BigDecimal quantity;
}
