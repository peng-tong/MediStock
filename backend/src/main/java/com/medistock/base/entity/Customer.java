package com.medistock.base.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.medistock.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 客户 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_customer")
public class Customer extends BaseEntity {
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
