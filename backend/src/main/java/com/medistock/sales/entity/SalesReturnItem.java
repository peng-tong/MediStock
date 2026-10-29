package com.medistock.sales.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.medistock.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/** 销售退货明细 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sales_return_item")
public class SalesReturnItem extends BaseEntity {
    private Long id;
    private Long tenantId;
    private Long returnId;
    private Long drugId;
    private Long batchId;        // 回补批次（退回哪个批次）
    private BigDecimal quantity;
    private BigDecimal returnPrice;
    private BigDecimal amount;
}
