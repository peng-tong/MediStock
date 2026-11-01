package com.medistock.base.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.base.entity.Warehouse;
import com.medistock.base.mapper.WarehouseMapper;
import com.medistock.common.config.TenantContext;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.inventory.entity.Stock;
import com.medistock.inventory.entity.Stocktake;
import com.medistock.inventory.mapper.StockMapper;
import com.medistock.inventory.mapper.StocktakeMapper;
import com.medistock.purchase.entity.PurchaseOrder;
import com.medistock.purchase.entity.PurchaseReturn;
import com.medistock.purchase.mapper.PurchaseOrderMapper;
import com.medistock.purchase.mapper.PurchaseReturnMapper;
import com.medistock.sales.entity.SalesOrder;
import com.medistock.sales.entity.SalesReturn;
import com.medistock.sales.mapper.SalesOrderMapper;
import com.medistock.sales.mapper.SalesReturnMapper;
import org.springframework.stereotype.Service;

/** 仓库：简单 CRUD */
@Service
public class WarehouseService extends ServiceImpl<WarehouseMapper, Warehouse> {

    private final StockMapper stockMapper;
    private final StocktakeMapper stocktakeMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseReturnMapper purchaseReturnMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final SalesReturnMapper salesReturnMapper;

    public WarehouseService(WarehouseMapper warehouseMapper, StockMapper stockMapper,
                            StocktakeMapper stocktakeMapper, PurchaseOrderMapper purchaseOrderMapper,
                            PurchaseReturnMapper purchaseReturnMapper, SalesOrderMapper salesOrderMapper,
                            SalesReturnMapper salesReturnMapper) {
        // ServiceImpl 在 MP 3.5.7 无带参构造器，直接为 protected baseMapper 赋值（同 Phase 1 UserService 修正方式）
        this.baseMapper = warehouseMapper;
        this.stockMapper = stockMapper;
        this.stocktakeMapper = stocktakeMapper;
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseReturnMapper = purchaseReturnMapper;
        this.salesOrderMapper = salesOrderMapper;
        this.salesReturnMapper = salesReturnMapper;
    }

    /** 新增：非超管清掉请求体里的 tenant_id（由租户拦截器在 INSERT 时注入当前租户）；超管未传时补默认值防 NOT NULL 违反 */
    public void createWarehouse(Warehouse w) {
        validateWarehouse(w);
        if (!TenantContext.isSuperAdmin()) {
            w.setTenantId(null);
        }
        if (w.getTenantId() == null) {
            w.setTenantId(TenantContext.getTenantId() == null ? 0L : TenantContext.getTenantId());
        }
        this.save(w);
    }

    /**
     * 更新：禁止修改 tenant_id（UPDATE 的 WHERE 由拦截器按当前租户追加，防跨租户改数据）
     * S5 修复：更新前先判存在，跨租户/不存在的 id 命中 0 行时改为 404，避免返回 200 假成功
     */
    public void updateWarehouse(Warehouse w) {
        w.setTenantId(null);
        if (this.getById(w.getId()) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "仓库不存在或无权限操作");
        }
        // 编辑时同样执行必填/长度/状态校验
        validateWarehouse(w);
        updateById(w);
    }

    /** 仓库字段校验：名称/编码必填，地址/负责人/备注按列宽限制，状态只能是 0/1 */
    private void validateWarehouse(Warehouse w) {
        w.setName(BaseDataValidator.requireText(w.getName(), "名称", 64));
        w.setCode(BaseDataValidator.requireText(w.getCode(), "编码", 32));
        w.setAddress(BaseDataValidator.optionalText(w.getAddress(), "地址", 255));
        w.setManager(BaseDataValidator.optionalText(w.getManager(), "负责人", 64));
        w.setRemark(BaseDataValidator.optionalText(w.getRemark(), "备注", 255));
        BaseDataValidator.checkStatus(w.getStatus());
    }

    /**
     * 删除：先判存在，不存在/跨租户统一 404（此前控制器直接 removeById，删不存在也返回 200）。
     * 同时校验库存/采购/销售/退货/盘点单据引用：仓库被引用时删除会让单据与库存变成孤儿，
     * 历史名称也无法回显，因此与供应商/客户保持一致的「被引用禁止删除」口径。
     */
    public void deleteWarehouse(Long id) {
        if (this.getById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "仓库不存在");
        }
        long referenced =
                stockMapper.selectCount(new LambdaQueryWrapper<Stock>().eq(Stock::getWarehouseId, id))
                + stocktakeMapper.selectCount(new LambdaQueryWrapper<Stocktake>().eq(Stocktake::getWarehouseId, id))
                + purchaseOrderMapper.selectCount(new LambdaQueryWrapper<PurchaseOrder>()
                        .eq(PurchaseOrder::getWarehouseId, id))
                + purchaseReturnMapper.selectCount(new LambdaQueryWrapper<PurchaseReturn>()
                        .eq(PurchaseReturn::getWarehouseId, id))
                + salesOrderMapper.selectCount(new LambdaQueryWrapper<SalesOrder>()
                        .eq(SalesOrder::getWarehouseId, id))
                + salesReturnMapper.selectCount(new LambdaQueryWrapper<SalesReturn>()
                        .eq(SalesReturn::getWarehouseId, id));
        if (referenced > 0) {
            throw new BusinessException(ErrorCode.SUPPLIER_CUSTOMER_REFERENCED,
                    "仓库已被库存或单据引用，禁止删除");
        }
        removeById(id);
    }
}
