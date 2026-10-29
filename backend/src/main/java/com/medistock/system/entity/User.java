package com.medistock.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.medistock.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 用户 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {
    private Long id;
    private Long tenantId;
    private String username;
    /** BCrypt 密文，任何接口不得返回 */
    @TableField(select = false)   // 默认查询不返回密码
    private String password;
    private String realName;
    private String phone;
    private String email;
    /** 0-禁用 1-正常 */
    private Integer status;
    private LocalDateTime lastLoginTime;
}
