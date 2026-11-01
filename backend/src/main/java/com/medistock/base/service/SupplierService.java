package com.medistock.base.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.base.entity.Supplier;
import com.medistock.base.mapper.SupplierMapper;
import com.medistock.common.config.TenantContext;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.purchase.entity.PurchaseOrder;
import com.medistock.purchase.entity.PurchaseReturn;
import com.medistock.purchase.mapper.PurchaseOrderMapper;
import com.medistock.purchase.mapper.PurchaseReturnMapper;
import org.springframework.stereotype.Service;

/** 供应商：简单 CRUD */
@Service
public class SupplierService extends ServiceImpl<SupplierMapper, Supplier> {

    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseReturnMapper purchaseReturnMapper;

    public SupplierService(SupplierMapper supplierMapper, PurchaseOrderMapper purchaseOrderMapper,
                           PurchaseReturnMapper purchaseReturnMapper) {
        // ServiceImpl 在 MP 3.5.7 无带参构造器，直接为 protected baseMapper 赋值（同 Phase 1 UserService 修正方式）
        this.baseMapper = supplierMapper;
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseReturnMapper = purchaseReturnMapper;
    }

    /** 新增：非超管清掉请求体里的 tenant_id（由租户拦截器在 INSERT 时注入当前租户）；超管未传时补默认值防 NOT NULL 违反 */
    public void createSupplier(Supplier s) {
        validateSupplier(s);
        if (!TenantContext.isSuperAdmin()) {
            s.setTenantId(null);
        }
        if (s.getTenantId() == null) {
            s.setTenantId(TenantContext.getTenantId() == null ? 0L : TenantContext.getTenantId());
        }
        this.save(s);
    }

    /**
     * 更新：禁止修改 tenant_id（UPDATE 的 WHERE 由拦截器按当前租户追加，防跨租户改数据）
     * S5 修复：更新前先判存在，跨租户/不存在的 id 命中 0 行时改为 404，避免返回 200 假成功
     */
    public void updateSupplier(Supplier s) {
        s.setTenantId(null);
        if (this.getById(s.getId()) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "供应商不存在或无权限操作");
        }
        // 编辑时同样执行必填/长度/状态校验
        validateSupplier(s);
        updateById(s);
    }

    /** 供应商字段校验：名称/编码必填，其余文本按列宽限制，状态只能是 0/1 */
    private void validateSupplier(Supplier s) {
        s.setName(BaseDataValidator.requireText(s.getName(), "名称", 128));
        s.setCode(BaseDataValidator.requireText(s.getCode(), "编码", 32));
        s.setContactName(BaseDataValidator.optionalText(s.getContactName(), "联系人", 64));
        s.setContactPhone(BaseDataValidator.optionalText(s.getContactPhone(), "联系电话", 32));
        s.setAddress(BaseDataValidator.optionalText(s.getAddress(), "地址", 255));
        s.setRemark(BaseDataValidator.optionalText(s.getRemark(), "备注", 255));
        BaseDataValidator.checkStatus(s.getStatus());
    }

    /** 删除供应商：被采购单引用的供应商禁止删除（防孤儿/明细名称悬空） */
    public void deleteSupplier(Long id) {
        // 先判存在：删除不存在/跨租户的供应商此前返回 200 ok，改为统一 404
        if (this.getById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "供应商不存在");
        }
        // S10 防孤儿：purchase_order / purchase_return 仍有该供应商单据时拦截（selectCount 租户自动隔离）
        if (purchaseOrderMapper.selectCount(new LambdaQueryWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getSupplierId, id)) > 0
                || purchaseReturnMapper.selectCount(new LambdaQueryWrapper<PurchaseReturn>()
                .eq(PurchaseReturn::getSupplierId, id)) > 0) {
            throw new BusinessException(ErrorCode.SUPPLIER_CUSTOMER_REFERENCED);
        }
        removeById(id);
    }
}
