package com.medistock.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 用户-角色关联（带 tenant_id） */
@Data
@TableName("sys_user_role")
public class UserRole {
    private Long id;
    private Long tenantId;
    private Long userId;
    private Long roleId;
}
