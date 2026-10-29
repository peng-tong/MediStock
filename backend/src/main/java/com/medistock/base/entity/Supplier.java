package com.medistock.base.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.medistock.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 供应商 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_supplier")
public class Supplier extends BaseEntity {
    private Long id;
    private Long tenantId;
    private String name;
    private String code;
    private String contactName;
    private String contactPhone;
    private String address;
    private Integer status;
    private String remark;
}
