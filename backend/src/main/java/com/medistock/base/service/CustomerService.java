package com.medistock.base.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.base.entity.Customer;
import com.medistock.base.mapper.CustomerMapper;
import com.medistock.common.config.TenantContext;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.sales.entity.SalesOrder;
import com.medistock.sales.entity.SalesReturn;
import com.medistock.sales.mapper.SalesOrderMapper;
import com.medistock.sales.mapper.SalesReturnMapper;
import org.springframework.stereotype.Service;

/** 客户：简单 CRUD */
@Service
public class CustomerService extends ServiceImpl<CustomerMapper, Customer> {

    private final SalesOrderMapper salesOrderMapper;
    private final SalesReturnMapper salesReturnMapper;

    public CustomerService(CustomerMapper customerMapper, SalesOrderMapper salesOrderMapper,
                           SalesReturnMapper salesReturnMapper) {
        // ServiceImpl 在 MP 3.5.7 无带参构造器，直接为 protected baseMapper 赋值（同 Phase 1 UserService 修正方式）
        this.baseMapper = customerMapper;
        this.salesOrderMapper = salesOrderMapper;
        this.salesReturnMapper = salesReturnMapper;
    }

    /** 新增：非超管清掉请求体里的 tenant_id（由租户拦截器在 INSERT 时注入当前租户）；超管未传时补默认值防 NOT NULL 违反 */
    public void createCustomer(Customer c) {
        validateCustomer(c);
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
    public void updateCustomer(Customer c) {
        c.setTenantId(null);
        if (this.getById(c.getId()) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "客户不存在或无权限操作");
        }
        // 编辑时同样执行必填/长度/状态校验
        validateCustomer(c);
        updateById(c);
    }

    /** 客户字段校验：名称/编码必填，其余文本按列宽限制，状态只能是 0/1 */
    private void validateCustomer(Customer c) {
        c.setName(BaseDataValidator.requireText(c.getName(), "名称", 128));
        c.setCode(BaseDataValidator.requireText(c.getCode(), "编码", 32));
        c.setContactName(BaseDataValidator.optionalText(c.getContactName(), "联系人", 64));
        c.setContactPhone(BaseDataValidator.optionalText(c.getContactPhone(), "联系电话", 32));
        c.setAddress(BaseDataValidator.optionalText(c.getAddress(), "地址", 255));
        c.setRemark(BaseDataValidator.optionalText(c.getRemark(), "备注", 255));
        BaseDataValidator.checkStatus(c.getStatus());
    }

    /** 删除客户：被销售单引用的客户禁止删除（防孤儿/明细名称悬空） */
    public void deleteCustomer(Long id) {
        // 先判存在：删除不存在/跨租户的客户此前返回 200 ok，改为统一 404
        if (this.getById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "客户不存在");
        }
        // S10 防孤儿：sales_order / sales_return 仍有该客户单据时拦截（selectCount 租户自动隔离）
        if (salesOrderMapper.selectCount(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getCustomerId, id)) > 0
                || salesReturnMapper.selectCount(new LambdaQueryWrapper<SalesReturn>()
                .eq(SalesReturn::getCustomerId, id)) > 0) {
            throw new BusinessException(ErrorCode.SUPPLIER_CUSTOMER_REFERENCED);
        }
        removeById(id);
    }
}
