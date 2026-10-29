package com.medistock.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.medistock.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

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
}
