package com.medistock.base.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.medistock.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/** 药品档案 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_drug")
public class Drug extends BaseEntity {
    private Long id;
    private Long tenantId;
    private Long categoryId;
    private String genericName;   // 通用名
    private String tradeName;     // 商品名
    private String spec;          // 规格
    private String dosageForm;    // 剂型
    private String manufacturer;  // 生产厂家
    private String approvalNo;    // 批准文号
    private String barcode;       // 条码
    private String storageCondition; // 存储条件
    private BigDecimal minStock;  // 库存下限【P1 预警预留】
    private BigDecimal maxStock;  // 库存上限【P1 预警预留】
    private Integer status;       // 0-停用 1-启用
    private String remark;
}
