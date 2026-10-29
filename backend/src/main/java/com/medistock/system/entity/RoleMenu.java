package com.medistock.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 角色-菜单关联（带 tenant_id，纳入租户隔离） */
@Data
@TableName("sys_role_menu")
public class RoleMenu {
    private Long id;
    private Long tenantId;
    private Long roleId;
    private Long menuId;
}
