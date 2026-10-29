package com.medistock.base.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.base.entity.DrugCategory;
import com.medistock.base.mapper.DrugCategoryMapper;
import com.medistock.common.config.TenantContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 药品分类管理：树形结构 */
@Service
public class DrugCategoryService extends ServiceImpl<DrugCategoryMapper, DrugCategory> {

    /** 返回整棵分类树（按 sort 升序） */
    public List<DrugCategory> tree() {
        List<DrugCategory> all = this.list(new LambdaQueryWrapper<DrugCategory>().orderByAsc(DrugCategory::getSort));
        Map<Long, List<DrugCategory>> byParent = all.stream().collect(Collectors.groupingBy(DrugCategory::getParentId));
        return buildTree(0L, byParent);
    }

    private List<DrugCategory> buildTree(Long parentId, Map<Long, List<DrugCategory>> byParent) {
        List<DrugCategory> children = new ArrayList<>();
        for (DrugCategory c : byParent.getOrDefault(parentId, List.of())) {
            c.setChildren(buildTree(c.getId(), byParent));
            children.add(c);
        }
        return children;
    }

    /** 新增：非超管清掉请求体里的 tenant_id（由租户拦截器在 INSERT 时注入当前租户）；超管未传时补默认值防 NOT NULL 违反 */
    public void createDrugCategory(DrugCategory c) {
        if (!TenantContext.isSuperAdmin()) {
            c.setTenantId(null);
        }
        if (c.getTenantId() == null) {
            c.setTenantId(TenantContext.getTenantId() == null ? 0L : TenantContext.getTenantId());
        }
        this.save(c);
    }

    /** 更新：禁止修改 tenant_id（UPDATE 的 WHERE 由拦截器按当前租户追加，防跨租户改数据） */
    public void updateDrugCategory(DrugCategory c) {
        c.setTenantId(null);
        updateById(c);
    }
}
