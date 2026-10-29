package com.medistock.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.medistock.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 租户（平台级表：租户拦截器已忽略） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant")
public class Tenant extends BaseEntity {
    private Long id;
    private String name;
    private String code;
    /** 0-停用 1-启用 */
    private Integer status;
    /** 最大用户数限制 */
    private Integer maxUserCount;
    private String contactName;
    private String contactPhone;
    private LocalDateTime expireTime;
}
