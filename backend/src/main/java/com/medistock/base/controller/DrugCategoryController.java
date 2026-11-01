package com.medistock.base.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medistock.base.entity.DrugCategory;
import com.medistock.base.service.DrugCategoryService;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.common.result.PageResult;
import com.medistock.common.result.Result;
import com.medistock.common.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 药品分类接口：分类树 / 分页 / 增删改 */
@RestController
@RequestMapping("/api/drug-categories")
@RequiredArgsConstructor
public class DrugCategoryController {

    private final DrugCategoryService service;

    @GetMapping("/tree")
    public Result<List<DrugCategory>> tree() { return Result.ok(service.tree()); }

    @GetMapping
    public Result<PageResult<DrugCategory>> page(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int pageSize,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Integer status) {
        // 关键字模糊匹配名称/编码，状态过滤，按 sort 升序（与分类树一致）
        LambdaQueryWrapper<DrugCategory> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(DrugCategory::getName, keyword).or().like(DrugCategory::getCode, keyword));
        }
        if (status != null) {
            qw.eq(DrugCategory::getStatus, status);
        }
        qw.orderByAsc(DrugCategory::getSort);
        Page<DrugCategory> p = service.page(new Page<>(PageUtil.page(page), PageUtil.pageSize(pageSize)), qw);
        return Result.ok(new PageResult<>(p.getTotal(), p.getRecords()));
    }

    /** 详情：存在返回 200，不存在/跨租户返回 404（补齐此前缺失导致 500 的接口） */
    @GetMapping("/{id}")
    public Result<DrugCategory> detail(@PathVariable Long id) {
        DrugCategory c = service.getById(id);
        if (c == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        return Result.ok(c);
    }

    @PostMapping
    public Result<Void> create(@RequestBody DrugCategory c) { service.createDrugCategory(c); return Result.ok(); }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody DrugCategory c) {
        c.setId(id); service.updateDrugCategory(c); return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { service.deleteCategory(id); return Result.ok(); }
}
