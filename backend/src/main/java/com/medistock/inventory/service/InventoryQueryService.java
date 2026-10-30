package com.medistock.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medistock.base.entity.Drug;
import com.medistock.common.service.NameResolver;
import com.medistock.inventory.entity.InventoryFlow;
import com.medistock.inventory.entity.Stock;
import com.medistock.inventory.entity.StockBatch;
import com.medistock.inventory.mapper.StockMapper;
import com.medistock.inventory.mapper.StockBatchMapper;
import com.medistock.inventory.mapper.InventoryFlowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 库存查询：按药品 / 仓库 / 批次维度查实时库存与流水（列表统一回填展示名称） */
@Service
@RequiredArgsConstructor
public class InventoryQueryService {

    private final StockMapper stockMapper;
    private final StockBatchMapper batchMapper;
    private final InventoryFlowMapper flowMapper;
    private final NameResolver nameResolver;
    private final com.medistock.base.mapper.DrugMapper drugMapper;

    public Page<Stock> pageStock(int page, int pageSize, Long drugId, Long warehouseId, String keyword) {
        LambdaQueryWrapper<Stock> qw = new LambdaQueryWrapper<>();
        if (drugId != null) qw.eq(Stock::getDrugId, drugId);
        if (warehouseId != null) qw.eq(Stock::getWarehouseId, warehouseId);
        // 关键字搜索：先按药品名匹配出药品 id 集合，再过滤库存
        if (keyword != null && !keyword.isBlank()) {
            List<Long> matched = drugIdsByKeyword(keyword);
            if (matched.isEmpty()) return new Page<>(page, pageSize);  // 无匹配返回空页
            qw.in(Stock::getDrugId, matched);
        }
        qw.orderByDesc(Stock::getUpdateTime);
        Page<Stock> p = stockMapper.selectPage(new Page<>(page, pageSize), qw);
        List<Stock> list = p.getRecords();
        if (list.isEmpty()) return p;
        // 批量补药品名/规格/仓库名
        Map<Long, Drug> drugs = nameResolver.drugs(ids(list, Stock::getDrugId));
        Map<Long, String> warehouses = nameResolver.warehouseNames(ids(list, Stock::getWarehouseId));
        list.forEach(s -> {
            Drug d = drugs.get(s.getDrugId());
            if (d != null) {
                s.setDrugName(d.getGenericName());
                s.setSpec(d.getSpec());
            }
            s.setWarehouseName(warehouses.get(s.getWarehouseId()));
        });
        return p;
    }

    public Page<StockBatch> pageBatch(int page, int pageSize, Long drugId, Long warehouseId,
                                      String keyword, String level) {
        LambdaQueryWrapper<StockBatch> qw = new LambdaQueryWrapper<>();
        if (drugId != null) qw.eq(StockBatch::getDrugId, drugId);
        if (warehouseId != null) qw.eq(StockBatch::getWarehouseId, warehouseId);
        // 关键字：药品名 或 生产批号
        if (keyword != null && !keyword.isBlank()) {
            List<Long> matched = drugIdsByKeyword(keyword);
            if (matched.isEmpty()) {
                qw.like(StockBatch::getBatchNo, keyword);
            } else {
                qw.and(w -> w.in(StockBatch::getDrugId, matched).or().like(StockBatch::getBatchNo, keyword));
            }
        }
        // 效期筛选：expired 已过期 / 90 近效期90天 / 30 近效期30天 / low 低于库存下限
        java.time.LocalDate today = java.time.LocalDate.now();
        if ("expired".equals(level)) {
            qw.lt(StockBatch::getExpiryDate, today);
        } else if ("90".equals(level)) {
            qw.between(StockBatch::getExpiryDate, today, today.plusDays(90));
        } else if ("30".equals(level)) {
            qw.between(StockBatch::getExpiryDate, today, today.plusDays(30));
        } else if ("low".equals(level)) {
            List<Long> lowIds = drugIdsBelowMinStock();
            if (lowIds.isEmpty()) return new Page<>(page, pageSize);
            qw.in(StockBatch::getDrugId, lowIds);
        }
        qw.orderByAsc(StockBatch::getExpiryDate);
        Page<StockBatch> p = batchMapper.selectPage(new Page<>(page, pageSize), qw);
        List<StockBatch> list = p.getRecords();
        if (list.isEmpty()) return p;
        Map<Long, String> drugs = nameResolver.drugNames(ids(list, StockBatch::getDrugId));
        Map<Long, String> warehouses = nameResolver.warehouseNames(ids(list, StockBatch::getWarehouseId));
        list.forEach(b -> {
            b.setDrugName(drugs.get(b.getDrugId()));
            b.setWarehouseName(warehouses.get(b.getWarehouseId()));
        });
        return p;
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
        Page<InventoryFlow> p = flowMapper.selectPage(new Page<>(page, pageSize), qw);
        List<InventoryFlow> list = p.getRecords();
        if (list.isEmpty()) return p;
        Map<Long, String> drugs = nameResolver.drugNames(ids(list, InventoryFlow::getDrugId));
        Map<Long, String> warehouses = nameResolver.warehouseNames(ids(list, InventoryFlow::getWarehouseId));
        Map<Long, String> operators = nameResolver.userNames(ids(list, InventoryFlow::getOperator));
        // 流水补批号：一次查完涉及的批次
        List<Long> batchIds = list.stream().map(InventoryFlow::getBatchId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> batches = batchIds.isEmpty() ? Map.of()
                : batchMapper.selectBatchIds(batchIds).stream()
                        .collect(Collectors.toMap(StockBatch::getId, StockBatch::getBatchNo, (a, b) -> a));
        list.forEach(f -> {
            f.setDrugName(drugs.get(f.getDrugId()));
            f.setWarehouseName(warehouses.get(f.getWarehouseId()));
            f.setOperatorName(operators.get(f.getOperator()));
            f.setBatchNo(batches.get(f.getBatchId()));
        });
        return p;
    }

    /** 提取列表里的 id 集合，供批量名称查询 */
    private <T> List<Long> ids(List<T> list, java.util.function.Function<T, Long> getter) {
        return list.stream().map(getter).filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
    }

    /** 关键字匹配药品 id：通用名 / 商品名 模糊匹配 */
    private List<Long> drugIdsByKeyword(String keyword) {
        return drugMapper.selectList(new LambdaQueryWrapper<Drug>()
                        .select(Drug::getId)
                        .like(Drug::getGenericName, keyword)
                        .or()
                        .like(Drug::getTradeName, keyword))
                .stream().map(Drug::getId).collect(Collectors.toList());
    }

    /** 低于库存下限的药品 id：按批次库存汇总后与药品下限比较 */
    private List<Long> drugIdsBelowMinStock() {
        List<Drug> drugs = drugMapper.selectList(new LambdaQueryWrapper<Drug>()
                .isNotNull(Drug::getMinStock));
        if (drugs.isEmpty()) return List.of();
        List<Long> drugIds = drugs.stream().map(Drug::getId).toList();
        // 汇总各药品的批次库存总量
        Map<Long, java.math.BigDecimal> totals = new java.util.HashMap<>();
        batchMapper.selectList(new LambdaQueryWrapper<StockBatch>()
                        .select(StockBatch::getDrugId, StockBatch::getQuantity)
                        .in(StockBatch::getDrugId, drugIds))
                .forEach(b -> totals.merge(b.getDrugId(),
                        b.getQuantity() == null ? java.math.BigDecimal.ZERO : b.getQuantity(),
                        java.math.BigDecimal::add));
        return drugs.stream()
                .filter(d -> totals.getOrDefault(d.getId(), java.math.BigDecimal.ZERO)
                        .compareTo(d.getMinStock()) < 0)
                .map(Drug::getId).collect(Collectors.toList());
    }
}
