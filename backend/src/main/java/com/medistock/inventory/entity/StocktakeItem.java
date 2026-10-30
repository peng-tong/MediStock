package com.medistock.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.medistock.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 盘点明细 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_stocktake_item")
public class StocktakeItem extends BaseEntity {
    private Long id;
    private Long tenantId;
    private Long takeId;
    private Long drugId;
    private Long batchId;
    private BigDecimal bookQuantity;    // 账面数量（快照）
    private BigDecimal actualQuantity;  // 实盘数量
    private BigDecimal diffQuantity;    // 差异 = 实盘 - 账面
    /** 展示字段：药品名称 / 规格 / 批号 / 有效期 / 成本价（非表字段） */
    @TableField(exist = false)
    private String drugName;
    @TableField(exist = false)
    private String spec;
    @TableField(exist = false)
    private String batchNo;
    @TableField(exist = false)
    private LocalDate expiryDate;
    @TableField(exist = false)
    private BigDecimal price;
}
