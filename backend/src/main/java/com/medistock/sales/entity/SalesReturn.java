package com.medistock.sales.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.medistock.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/** 销售退货单头 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sales_return")
public class SalesReturn extends BaseEntity {
    private Long id;
    private Long tenantId;
    private String returnNo;
    private Long orderId;        // 关联销售单（可为空，支持直接退货）
    private Long customerId;
    private Long warehouseId;
    private BigDecimal totalAmount;
    /** 0草稿 1已完成 2已取消 */
    private Integer status;
    private String remark;
    /** 展示字段：客户名称 / 经办人姓名（非表字段） */
    @TableField(exist = false)
    private String customerName;
    @TableField(exist = false)
    private String createByName;
}
