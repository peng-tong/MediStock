package com.medistock.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.medistock.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 批次库存：生产批号 + 有效期（药品行业核心），乐观锁版本字段 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_stock_batch")
public class StockBatch extends BaseEntity {
    private Long id;
    private Long tenantId;
    private Long drugId;
    private Long warehouseId;
    private String batchNo;
    private LocalDate expiryDate;
    private BigDecimal quantity;
    private LocalDate inDate;
    /** 0-正常 1-已过期（定时任务标记，P1 口子） */
    private Integer status;
    /** 乐观锁版本号（CAS 核心） */
    @Version
    private Integer version;
}
