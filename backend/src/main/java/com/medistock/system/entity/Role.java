package com.medistock.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.medistock.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 角色 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class Role extends BaseEntity {
    private Long id;
    private Long tenantId;
    private String name;
    /** SUPER_ADMIN / TENANT_ADMIN / 自定义 */
    private String code;
    private Integer status;
    private String remark;
}
