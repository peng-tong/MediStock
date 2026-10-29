package com.medistock.base.controller;

import com.medistock.base.entity.Supplier;
import com.medistock.base.service.SupplierService;
import com.medistock.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 供应商接口：列表 / 增删改 */
@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService service;

    @GetMapping
    public Result<List<Supplier>> list() { return Result.ok(service.list()); }

    @PostMapping
    public Result<Void> create(@RequestBody Supplier s) { service.createSupplier(s); return Result.ok(); }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Supplier s) {
        s.setId(id); service.updateSupplier(s); return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { service.removeById(id); return Result.ok(); }
}
