package com.medistock.base.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.base.entity.Warehouse;
import com.medistock.base.mapper.WarehouseMapper;
import com.medistock.common.config.TenantContext;
import org.springframework.stereotype.Service;

/** 仓库：简单 CRUD */
@Service
public class WarehouseService extends ServiceImpl<WarehouseMapper, Warehouse> {

    /** 新增：非超管清掉请求体里的 tenant_id（由租户拦截器在 INSERT 时注入当前租户）；超管未传时补默认值防 NOT NULL 违反 */
    public void createWarehouse(Warehouse w) {
        if (!TenantContext.isSuperAdmin()) {
            w.setTenantId(null);
        }
        if (w.getTenantId() == null) {
            w.setTenantId(TenantContext.getTenantId() == null ? 0L : TenantContext.getTenantId());
        }
        this.save(w);
    }

    /** 更新：禁止修改 tenant_id（UPDATE 的 WHERE 由拦截器按当前租户追加，防跨租户改数据） */
    public void updateWarehouse(Warehouse w) {
        w.setTenantId(null);
        updateById(w);
    }
}
