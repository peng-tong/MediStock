package com.medistock.base.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.base.entity.Drug;
import com.medistock.base.mapper.DrugMapper;
import com.medistock.common.config.TenantContext;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.inventory.entity.Stock;
import com.medistock.inventory.mapper.StockMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/** 药品档案：分页查询 / 增删改（有库存时禁止删除的防呆校验） */
@Service
public class DrugService extends ServiceImpl<DrugMapper, Drug> {

    private final StockMapper stockMapper;

    public DrugService(DrugMapper drugMapper, StockMapper stockMapper) {
        // ServiceImpl 在 MP 3.5.7 无带参构造器，直接为 protected baseMapper 赋值（同 Phase 1 UserService 修正方式）
        this.baseMapper = drugMapper;
        this.stockMapper = stockMapper;
    }

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

    /** 删除药品：存在库存时禁止删除（防呆校验） */
    public void deleteDrug(Long id) {
        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getDrugId, id));
        if (stock != null && stock.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException(ErrorCode.DRUG_IN_STOCK);
        }
        removeById(id);
    }
}
