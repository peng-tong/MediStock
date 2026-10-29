package com.medistock.base.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.base.entity.Supplier;
import com.medistock.base.mapper.SupplierMapper;
import com.medistock.common.config.TenantContext;
import org.springframework.stereotype.Service;

/** 供应商：简单 CRUD */
@Service
public class SupplierService extends ServiceImpl<SupplierMapper, Supplier> {

    /** 新增：非超管清掉请求体里的 tenant_id（由租户拦截器在 INSERT 时注入当前租户）；超管未传时补默认值防 NOT NULL 违反 */
    public void createSupplier(Supplier s) {
        if (!TenantContext.isSuperAdmin()) {
            s.setTenantId(null);
        }
        if (s.getTenantId() == null) {
            s.setTenantId(TenantContext.getTenantId() == null ? 0L : TenantContext.getTenantId());
        }
        this.save(s);
    }

    /** 更新：禁止修改 tenant_id（UPDATE 的 WHERE 由拦截器按当前租户追加，防跨租户改数据） */
    public void updateSupplier(Supplier s) {
        s.setTenantId(null);
        updateById(s);
    }
}
