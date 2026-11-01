package com.medistock.base.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.base.entity.Drug;
import com.medistock.base.entity.DrugCategory;
import com.medistock.base.mapper.DrugCategoryMapper;
import com.medistock.base.mapper.DrugMapper;
import com.medistock.common.config.TenantContext;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 药品分类管理：树形结构 */
@Service
public class DrugCategoryService extends ServiceImpl<DrugCategoryMapper, DrugCategory> {

    private final DrugMapper drugMapper;

    public DrugCategoryService(DrugCategoryMapper drugCategoryMapper, DrugMapper drugMapper) {
        // ServiceImpl 在 MP 3.5.7 无带参构造器，直接为 protected baseMapper 赋值（同 Phase 1 UserService 修正方式）
        this.baseMapper = drugCategoryMapper;
        this.drugMapper = drugMapper;
    }

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
        validateCategory(c);
        if (!TenantContext.isSuperAdmin()) {
            c.setTenantId(null);
        }
        if (c.getTenantId() == null) {
            c.setTenantId(TenantContext.getTenantId() == null ? 0L : TenantContext.getTenantId());
        }
        this.save(c);
    }

    /**
     * 更新：禁止修改 tenant_id（UPDATE 的 WHERE 由拦截器按当前租户追加，防跨租户改数据）
     * S5 修复：更新前先判存在，跨租户/不存在的 id 命中 0 行时改为 404，避免返回 200 假成功
     */
    public void updateDrugCategory(DrugCategory c) {
        c.setTenantId(null);
        if (this.getById(c.getId()) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在或无权限操作");
        }
        // 编辑时同样校验必填/长度/状态，避免通过 PUT 绕过前端约束
        validateCategory(c);
        updateById(c);
    }

    /** 分类字段校验：名称/编码为前端必填项，长度不超过列宽，状态只能是 0/1 */
    private void validateCategory(DrugCategory c) {
        c.setName(BaseDataValidator.requireText(c.getName(), "分类名称", 64));
        c.setCode(BaseDataValidator.requireText(c.getCode(), "编码", 32));
        BaseDataValidator.checkStatus(c.getStatus());
    }

    /** 删除分类：被药品引用的分类禁止删除（防孤儿分类/悬空名称） */
    public void deleteCategory(Long id) {
        // 先判存在：删除不存在/跨租户的分类此前返回 200 ok，改为统一 404
        if (this.getById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        // S10 防孤儿：base_drug 仍引用该分类时拦截（selectCount 租户自动隔离）
        if (drugMapper.selectCount(new LambdaQueryWrapper<Drug>().eq(Drug::getCategoryId, id)) > 0) {
            throw new BusinessException(ErrorCode.SUPPLIER_CUSTOMER_REFERENCED);
        }
        removeById(id);
    }
}
