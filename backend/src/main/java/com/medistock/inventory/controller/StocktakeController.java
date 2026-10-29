package com.medistock.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medistock.common.result.PageResult;
import com.medistock.common.result.Result;
import com.medistock.inventory.entity.Stocktake;
import com.medistock.inventory.service.StocktakeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/** 盘点接口：创建 / 分页 / 详情 / 录入实盘 / 确认 / 取消（路径按设计文档 8.2） */
@RestController
@RequestMapping("/api/inventory/stocktakes")
@RequiredArgsConstructor
public class StocktakeController {

    private final StocktakeService service;

    @PostMapping
    public Result<Long> create(@RequestParam Long warehouseId,
                               @RequestParam(required = false) String remark) {
        return Result.ok(service.createStocktake(warehouseId, remark));
    }

    @GetMapping
    public Result<PageResult<Stocktake>> page(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int pageSize) {
        Page<Stocktake> p = service.page(new Page<>(page, pageSize), null);
        return Result.ok(new PageResult<>(p.getTotal(), p.getRecords()));
    }

    @GetMapping("/{id}")
    public Result<Stocktake> detail(@PathVariable Long id) { return Result.ok(service.getById(id)); }

    @PutMapping("/{id}/items/{itemId}")
    public Result<Void> updateActual(@PathVariable Long id, @PathVariable Long itemId,
                                     @RequestParam BigDecimal actualQuantity) {
        service.updateActual(itemId, actualQuantity);
        return Result.ok();
    }

    @PostMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) { service.confirm(id); return Result.ok(); }

    @DeleteMapping("/{id}")
    public Result<Void> cancel(@PathVariable Long id) { service.removeById(id); return Result.ok(); }
}
