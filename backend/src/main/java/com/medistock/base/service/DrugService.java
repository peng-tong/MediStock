package com.medistock.base.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.base.entity.Drug;
import com.medistock.base.mapper.DrugCategoryMapper;
import com.medistock.base.mapper.DrugMapper;
import com.medistock.common.config.TenantContext;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.inventory.entity.Stock;
import com.medistock.inventory.entity.StocktakeItem;
import com.medistock.inventory.mapper.StockMapper;
import com.medistock.inventory.mapper.StocktakeItemMapper;
import com.medistock.purchase.entity.PurchaseOrderItem;
import com.medistock.purchase.entity.PurchaseReturnItem;
import com.medistock.purchase.mapper.PurchaseOrderItemMapper;
import com.medistock.purchase.mapper.PurchaseReturnItemMapper;
import com.medistock.sales.entity.SalesOrderItem;
import com.medistock.sales.entity.SalesReturnItem;
import com.medistock.sales.mapper.SalesOrderItemMapper;
import com.medistock.sales.mapper.SalesReturnItemMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/** 药品档案：分页查询 / 增删改（有库存时禁止删除的防呆校验） */
@Service
public class DrugService extends ServiceImpl<DrugMapper, Drug> {

    private final StockMapper stockMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final DrugCategoryMapper categoryMapper;
    private final PurchaseReturnItemMapper purchaseReturnItemMapper;
    private final SalesReturnItemMapper salesReturnItemMapper;
    private final StocktakeItemMapper stocktakeItemMapper;

    public DrugService(DrugMapper drugMapper, StockMapper stockMapper,
                       PurchaseOrderItemMapper purchaseOrderItemMapper,
                       SalesOrderItemMapper salesOrderItemMapper,
                       DrugCategoryMapper categoryMapper,
                       PurchaseReturnItemMapper purchaseReturnItemMapper,
                       SalesReturnItemMapper salesReturnItemMapper,
                       StocktakeItemMapper stocktakeItemMapper) {
        // ServiceImpl 在 MP 3.5.7 无带参构造器，直接为 protected baseMapper 赋值（同 Phase 1 UserService 修正方式）
        this.baseMapper = drugMapper;
        this.stockMapper = stockMapper;
        this.purchaseOrderItemMapper = purchaseOrderItemMapper;
        this.salesOrderItemMapper = salesOrderItemMapper;
        this.categoryMapper = categoryMapper;
        this.purchaseReturnItemMapper = purchaseReturnItemMapper;
        this.salesReturnItemMapper = salesReturnItemMapper;
        this.stocktakeItemMapper = stocktakeItemMapper;
    }

    public Page<Drug> pageQuery(int page, int pageSize, String keyword, Long categoryId, Integer status) {
        LambdaQueryWrapper<Drug> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(Drug::getGenericName, keyword).or().like(Drug::getTradeName, keyword)
                    .or().like(Drug::getBarcode, keyword));
        }
        if (categoryId != null) {
            qw.eq(Drug::getCategoryId, categoryId);
        }
        // 状态过滤：与供应商/客户/仓库/分类保持一致（此前未声明该参数，status=abc 会被静默忽略）
        if (status != null) {
            qw.eq(Drug::getStatus, status);
        }
        qw.orderByDesc(Drug::getId);
        return this.page(new Page<>(page, pageSize), qw);
    }

    /** 新增：非超管清掉请求体里的 tenant_id（由租户拦截器在 INSERT 时注入当前租户）；超管未传时补默认值防 NOT NULL 违反 */
    public void createDrug(Drug d) {
        // 前置校验：必填/长度/状态/库存阈值/分类归属，避免绕过前端写入脏数据
        validateDrug(d);
        if (!TenantContext.isSuperAdmin()) {
            d.setTenantId(null);
        }
        if (d.getTenantId() == null) {
            d.setTenantId(TenantContext.getTenantId() == null ? 0L : TenantContext.getTenantId());
        }
        this.save(d);
    }

    /**
     * 更新：禁止修改 tenant_id（UPDATE 的 WHERE 由拦截器按当前租户追加，防跨租户改数据）
     * S5 修复：更新前先判存在，跨租户/不存在的 id 直接被租户拦截器过滤为 0 行，
     * 此前仍返回 200 ok 造成「越权改名成功」的假象，改为明确 404
     */
    public void updateDrug(Drug d) {
        d.setTenantId(null);
        if (this.getById(d.getId()) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "药品不存在或无权限操作");
        }
        // 更新与新增使用同一套校验：状态、库存阈值、分类归属等都不能在编辑时被绕过
        validateDrug(d);
        updateById(d);
    }

    /**
     * 药品字段校验：
     * - 通用名/规格为前端必填项，trim 后不得为空，长度不超过列宽；
     * - 状态只能是 0/1，库存上下限非负且上限不小于下限；
     * - categoryId 必须指向当前租户下存在的分类（selectById 受租户拦截器过滤，跨租户返回 null）。
     */
    private void validateDrug(Drug d) {
        d.setGenericName(BaseDataValidator.requireText(d.getGenericName(), "通用名", 128));
        d.setSpec(BaseDataValidator.requireText(d.getSpec(), "规格", 64));
        d.setTradeName(BaseDataValidator.optionalText(d.getTradeName(), "商品名", 128));
        d.setDosageForm(BaseDataValidator.optionalText(d.getDosageForm(), "剂型", 32));
        d.setManufacturer(BaseDataValidator.optionalText(d.getManufacturer(), "生产厂家", 128));
        d.setApprovalNo(BaseDataValidator.optionalText(d.getApprovalNo(), "批准文号", 64));
        d.setBarcode(BaseDataValidator.optionalText(d.getBarcode(), "条码", 64));
        d.setStorageCondition(BaseDataValidator.optionalText(d.getStorageCondition(), "存储条件", 64));
        d.setRemark(BaseDataValidator.optionalText(d.getRemark(), "备注", 255));
        BaseDataValidator.checkStatus(d.getStatus());
        d.setMinStock(BaseDataValidator.optionalNonNegative(d.getMinStock(), "库存下限"));
        d.setMaxStock(BaseDataValidator.optionalNonNegative(d.getMaxStock(), "库存上限"));
        BaseDataValidator.checkStockRange(d.getMinStock(), d.getMaxStock());
        if (d.getCategoryId() != null) {
            if (d.getCategoryId() <= 0 || categoryMapper.selectById(d.getCategoryId()) == null) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "药品分类不存在或不属于当前租户");
            }
        }
    }

    /** 删除药品：存在库存时禁止删除（防呆校验） */
    public void deleteDrug(Long id) {
        // 先判存在：删除不存在/跨租户的药品此前返回 200 ok，改为统一 404（与用户删除口径一致）
        if (this.getById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "药品不存在");
        }
        // inv_stock 唯一键为 tenant+drug+warehouse，多仓库存行是常态，selectOne 会抛 TooManyResultsException，
        // 改为 selectList 后任一仓库有库存（数量 > 0）即拦截删除
        List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getDrugId, id));
        boolean hasStock = stocks.stream().anyMatch(s -> s.getQuantity() != null
                && s.getQuantity().compareTo(BigDecimal.ZERO) > 0);
        if (hasStock) {
            throw new BusinessException(ErrorCode.DRUG_IN_STOCK);
        }
        // S10 防孤儿：被采购/销售单据引用的药品禁止删除（selectCount 租户自动隔离）
        if (purchaseOrderItemMapper.selectCount(new LambdaQueryWrapper<PurchaseOrderItem>()
                .eq(PurchaseOrderItem::getDrugId, id)) > 0) {
            throw new BusinessException(ErrorCode.SUPPLIER_CUSTOMER_REFERENCED);
        }
        if (salesOrderItemMapper.selectCount(new LambdaQueryWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getDrugId, id)) > 0) {
            throw new BusinessException(ErrorCode.SUPPLIER_CUSTOMER_REFERENCED);
        }
        // 退货/盘点单据同样引用药品：一并拦截，防止历史单据只剩「药品#id」无法回显名称
        if (purchaseReturnItemMapper.selectCount(new LambdaQueryWrapper<PurchaseReturnItem>()
                .eq(PurchaseReturnItem::getDrugId, id)) > 0
                || salesReturnItemMapper.selectCount(new LambdaQueryWrapper<SalesReturnItem>()
                .eq(SalesReturnItem::getDrugId, id)) > 0
                || stocktakeItemMapper.selectCount(new LambdaQueryWrapper<StocktakeItem>()
                .eq(StocktakeItem::getDrugId, id)) > 0) {
            throw new BusinessException(ErrorCode.SUPPLIER_CUSTOMER_REFERENCED);
        }
        removeById(id);
    }
}
