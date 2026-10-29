package com.medistock.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.medistock.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/** 库存流水：一切出入库的原始凭据，全程可追溯 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_flow")
public class InventoryFlow extends BaseEntity {
    private Long id;
    private Long tenantId;
    private String flowNo;
    /** 业务类型：1采购入库 2采购退货 3销售出库 4销售退货 5盘点调整 6调拨(P1) */
    private Integer businessType;
    private String businessNo;
    private Long drugId;
    private Long warehouseId;
    private Long batchId;
    /** 变动数量：正入负出 */
    private BigDecimal changeQuantity;
    /** 变动前批次数量 */
    private BigDecimal beforeQuantity;
    /** 变动后批次数量 */
    private BigDecimal afterQuantity;
    private Long operator;
    private String remark;
}
