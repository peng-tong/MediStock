package com.medistock.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.medistock.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/** 菜单（平台级：租户拦截器已忽略）。type=3(按钮) 与 permission 为 P1 按钮权限预留 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class Menu extends BaseEntity {
    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private String component;
    private String icon;
    /** 1-目录 2-菜单 3-按钮(P1) */
    private Integer type;
    /** 权限标识，如 system:user:add（P1 接口鉴权用） */
    private String permission;
    private Integer sort;
    /** 0-隐藏 1-显示 */
    private Integer status;
    /** 子菜单（非表字段，仅树展示用） */
    @TableField(exist = false)
    private List<Menu> children = new ArrayList<>();
}
