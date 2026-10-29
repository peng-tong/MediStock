package com.medistock.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medistock.common.result.PageResult;
import com.medistock.common.result.Result;
import com.medistock.inventory.entity.InventoryFlow;
import com.medistock.inventory.entity.Stock;
import com.medistock.inventory.entity.StockBatch;
import com.medistock.inventory.service.InventoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/** 库存查询接口：实时库存 / 批次库存 / 流水（路径按设计文档 8.2） */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryQueryService queryService;

    @GetMapping("/stocks")
    public Result<PageResult<Stock>> stocks(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int pageSize,
                                            @RequestParam(required = false) Long drugId,
                                            @RequestParam(required = false) Long warehouseId) {
        Page<Stock> p = queryService.pageStock(page, pageSize, drugId, warehouseId);
        return Result.ok(new PageResult<>(p.getTotal(), p.getRecords()));
    }

    @GetMapping("/stocks/batches")
    public Result<PageResult<StockBatch>> batches(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int pageSize,
                                                  @RequestParam(required = false) Long drugId,
                                                  @RequestParam(required = false) Long warehouseId) {
        Page<StockBatch> p = queryService.pageBatch(page, pageSize, drugId, warehouseId);
        return Result.ok(new PageResult<>(p.getTotal(), p.getRecords()));
    }

    @GetMapping("/flows")
    public Result<PageResult<InventoryFlow>> flows(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int pageSize,
                                                   @RequestParam(required = false) Long drugId,
                                                   @RequestParam(required = false) Long warehouseId,
                                                   @RequestParam(required = false) Integer businessType,
                                                   // ISO 格式绑定：query string 形如 2026-10-29T00:00:00
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        Page<InventoryFlow> p = queryService.pageFlow(page, pageSize, drugId, warehouseId, businessType, start, end);
        return Result.ok(new PageResult<>(p.getTotal(), p.getRecords()));
    }
}
