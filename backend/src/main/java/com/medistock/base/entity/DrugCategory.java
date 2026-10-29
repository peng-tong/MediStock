package com.medistock.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.medistock.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/** 药品分类（处方药 / OTC 等） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_drug_category")
public class DrugCategory extends BaseEntity {
    private Long id;
    private Long tenantId;
    private Long parentId;
    private String name;
    private String code;
    private Integer sort;
    private Integer status;
    /** 子分类（非表字段，仅分类树展示用） */
    @TableField(exist = false)
    private List<DrugCategory> children = new ArrayList<>();
}
