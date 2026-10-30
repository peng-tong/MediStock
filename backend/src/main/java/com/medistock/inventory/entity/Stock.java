package com.medistock.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.medistock.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/** 实时库存汇总：一药一仓一条，乐观锁版本字段 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_stock")
public class Stock extends BaseEntity {
    private Long id;
    private Long tenantId;
    private Long drugId;
    private Long warehouseId;
    /** 当前库存总量（各批次之和） */
    private BigDecimal quantity;
    /** 可用数量（本期 = quantity，预留） */
    private BigDecimal availableQuantity;
    /** 乐观锁版本号（CAS 核心） */
    @Version
    private Integer version;
    /** 展示字段：药品名称 + 规格（非表字段，列表查询时批量回填） */
    @TableField(exist = false)
    private String drugName;
    @TableField(exist = false)
    private String spec;
    /** 展示字段：仓库名称 */
    @TableField(exist = false)
    private String warehouseName;
}
