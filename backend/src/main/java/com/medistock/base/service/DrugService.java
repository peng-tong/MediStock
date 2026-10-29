package com.medistock.base.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.base.entity.Drug;
import com.medistock.base.mapper.DrugMapper;
import com.medistock.common.config.TenantContext;
import org.springframework.stereotype.Service;

/** 药品档案：分页查询 / 增删改（删除保护延至 Task 6.1：有库存时禁止删除） */
@Service
public class DrugService extends ServiceImpl<DrugMapper, Drug> {

    public Page<Drug> pageQuery(int page, int pageSize, String keyword, Long categoryId) {
        LambdaQueryWrapper<Drug> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(Drug::getGenericName, keyword).or().like(Drug::getTradeName, keyword)
                    .or().like(Drug::getBarcode, keyword));
        }
        if (categoryId != null) {
            qw.eq(Drug::getCategoryId, categoryId);
        }
        qw.orderByDesc(Drug::getId);
        return this.page(new Page<>(page, pageSize), qw);
    }

    /** 新增：非超管清掉请求体里的 tenant_id（由租户拦截器在 INSERT 时注入当前租户）；超管未传时补默认值防 NOT NULL 违反 */
    public void createDrug(Drug d) {
        if (!TenantContext.isSuperAdmin()) {
            d.setTenantId(null);
        }
        if (d.getTenantId() == null) {
            d.setTenantId(TenantContext.getTenantId() == null ? 0L : TenantContext.getTenantId());
        }
        this.save(d);
    }

    /** 更新：禁止修改 tenant_id（UPDATE 的 WHERE 由拦截器按当前租户追加，防跨租户改数据） */
    public void updateDrug(Drug d) {
        d.setTenantId(null);
        updateById(d);
    }
}
