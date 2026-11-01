package com.medistock.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    /** BCrypt 密文，任何接口不得返回（WRITE_ONLY 只允许反序列化，序列化时不输出该字段） */
    @TableField(select = false)   // 默认查询不返回密码
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String realName;
    private String phone;
    private String email;
    /** 0-禁用 1-正常 */
    private Integer status;
    private LocalDateTime lastLoginTime;
    /** 展示字段：角色名称列表 / 所属租户名称（非表字段） */
    @TableField(exist = false)
    private java.util.List<String> roles;
    @TableField(exist = false)
    private String tenantName;
}
