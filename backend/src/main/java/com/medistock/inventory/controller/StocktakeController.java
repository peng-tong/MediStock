package com.medistock.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medistock.common.result.PageResult;
import com.medistock.common.result.Result;
import com.medistock.common.util.PageUtil;
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
        Page<Stocktake> p = service.pageQuery(PageUtil.page(page), PageUtil.pageSize(pageSize));
        return Result.ok(new PageResult<>(p.getTotal(), p.getRecords()));
    }

    @GetMapping("/{id}")
    public Result<Stocktake> detail(@PathVariable Long id) { return Result.ok(service.detail(id)); }

    @PutMapping("/{id}/items/{itemId}")
    public Result<Void> updateActual(@PathVariable Long id, @PathVariable Long itemId,
                                     @RequestParam BigDecimal actualQuantity) {
        // 必须把路径中的盘点单 id 一并传给服务层校验，防止通过篡改 itemId 修改其他盘点单明细
        service.updateActual(id, itemId, actualQuantity);
        return Result.ok();
    }

    @PostMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) { service.confirm(id); return Result.ok(); }

    @DeleteMapping("/{id}")
    public Result<Void> cancel(@PathVariable Long id) { service.cancel(id); return Result.ok(); }
}
