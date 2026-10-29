package com.medistock.inventory.entity;

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
}
