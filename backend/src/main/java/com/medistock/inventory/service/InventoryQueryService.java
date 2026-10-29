package com.medistock.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medistock.inventory.entity.Stock;
import com.medistock.inventory.entity.StockBatch;
import com.medistock.inventory.entity.InventoryFlow;
import com.medistock.inventory.mapper.StockMapper;
import com.medistock.inventory.mapper.StockBatchMapper;
import com.medistock.inventory.mapper.InventoryFlowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** 库存查询：按药品 / 仓库 / 批次维度查实时库存与流水 */
@Service
@RequiredArgsConstructor
public class InventoryQueryService {

    private final StockMapper stockMapper;
    private final StockBatchMapper batchMapper;
    private final InventoryFlowMapper flowMapper;

    public Page<Stock> pageStock(int page, int pageSize, Long drugId, Long warehouseId) {
        LambdaQueryWrapper<Stock> qw = new LambdaQueryWrapper<>();
        if (drugId != null) qw.eq(Stock::getDrugId, drugId);
        if (warehouseId != null) qw.eq(Stock::getWarehouseId, warehouseId);
        qw.orderByDesc(Stock::getUpdateTime);
        return stockMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    public Page<StockBatch> pageBatch(int page, int pageSize, Long drugId, Long warehouseId) {
        LambdaQueryWrapper<StockBatch> qw = new LambdaQueryWrapper<>();
        if (drugId != null) qw.eq(StockBatch::getDrugId, drugId);
        if (warehouseId != null) qw.eq(StockBatch::getWarehouseId, warehouseId);
        qw.orderByAsc(StockBatch::getExpiryDate);
        return batchMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    public Page<InventoryFlow> pageFlow(int page, int pageSize, Long drugId, Long warehouseId,
                                        Integer businessType, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<InventoryFlow> qw = new LambdaQueryWrapper<>();
        if (drugId != null) qw.eq(InventoryFlow::getDrugId, drugId);
        if (warehouseId != null) qw.eq(InventoryFlow::getWarehouseId, warehouseId);
        if (businessType != null) qw.eq(InventoryFlow::getBusinessType, businessType);
        if (start != null) qw.ge(InventoryFlow::getCreateTime, start);
        if (end != null) qw.le(InventoryFlow::getCreateTime, end);
        qw.orderByDesc(InventoryFlow::getId);
        return flowMapper.selectPage(new Page<>(page, pageSize), qw);
    }
}
