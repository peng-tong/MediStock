package com.medistock.base.controller;

import com.medistock.base.entity.Warehouse;
import com.medistock.base.service.WarehouseService;
import com.medistock.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 仓库接口：列表 / 增删改 */
@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService service;

    @GetMapping
    public Result<List<Warehouse>> list() { return Result.ok(service.list()); }

    @PostMapping
    public Result<Void> create(@RequestBody Warehouse w) { service.createWarehouse(w); return Result.ok(); }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Warehouse w) {
        w.setId(id); service.updateWarehouse(w); return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { service.removeById(id); return Result.ok(); }
}
