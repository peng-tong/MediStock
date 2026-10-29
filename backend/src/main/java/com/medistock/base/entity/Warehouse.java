package com.medistock.base.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.medistock.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 仓库（库位表 base_location 为 P1 预留，本期库存只到仓库级） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_warehouse")
public class Warehouse extends BaseEntity {
    private Long id;
    private Long tenantId;
    private String name;
    private String code;
    private String address;
    private String manager;
    private Integer status;
    private String remark;
}
