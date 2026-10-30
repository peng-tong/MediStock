package com.medistock.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.medistock.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 盘点单 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_stocktake")
public class Stocktake extends BaseEntity {
    private Long id;
    private Long tenantId;
    private String takeNo;
    private Long warehouseId;
    /** 0-进行中 1-已完成 2-已取消 */
    private Integer status;
    private Long createBy;
    private Long auditBy;   // 复核人（P1 预留）
    private String remark;
    /** 展示字段：仓库名称 / 药品品种数 / 盘点人姓名（非表字段） */
    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private Integer speciesCount;
    @TableField(exist = false)
    private String createByName;
    /** 详情返回的明细列表（非表字段） */
    @TableField(exist = false)
    private java.util.List<StocktakeItem> items;
}
