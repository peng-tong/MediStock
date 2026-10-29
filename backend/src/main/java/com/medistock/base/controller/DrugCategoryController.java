package com.medistock.base.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medistock.base.entity.DrugCategory;
import com.medistock.base.service.DrugCategoryService;
import com.medistock.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 药品分类接口：分类树 / 列表 / 增删改 */
@RestController
@RequestMapping("/api/drug-categories")
@RequiredArgsConstructor
public class DrugCategoryController {

    private final DrugCategoryService service;

    @GetMapping("/tree")
    public Result<List<DrugCategory>> tree() { return Result.ok(service.tree()); }

    @GetMapping
    public Result<List<DrugCategory>> list() {
        return Result.ok(service.list(new LambdaQueryWrapper<DrugCategory>().orderByAsc(DrugCategory::getSort)));
    }

    @PostMapping
    public Result<Void> create(@RequestBody DrugCategory c) { service.createDrugCategory(c); return Result.ok(); }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody DrugCategory c) {
        c.setId(id); service.updateDrugCategory(c); return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { service.removeById(id); return Result.ok(); }
}
