package com.medistock.base.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.base.entity.Customer;
import com.medistock.base.mapper.CustomerMapper;
import com.medistock.common.config.TenantContext;
import org.springframework.stereotype.Service;

/** 客户：简单 CRUD */
@Service
public class CustomerService extends ServiceImpl<CustomerMapper, Customer> {

    /** 新增：非超管清掉请求体里的 tenant_id（由租户拦截器在 INSERT 时注入当前租户）；超管未传时补默认值防 NOT NULL 违反 */
    public void createCustomer(Customer c) {
        if (!TenantContext.isSuperAdmin()) {
            c.setTenantId(null);
        }
        if (c.getTenantId() == null) {
            c.setTenantId(TenantContext.getTenantId() == null ? 0L : TenantContext.getTenantId());
        }
        this.save(c);
    }

    /** 更新：禁止修改 tenant_id（UPDATE 的 WHERE 由拦截器按当前租户追加，防跨租户改数据） */
    public void updateCustomer(Customer c) {
        c.setTenantId(null);
        updateById(c);
    }
}
