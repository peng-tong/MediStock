package com.medistock.base.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medistock.base.entity.Supplier;
import com.medistock.base.service.SupplierService;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.common.result.PageResult;
import com.medistock.common.result.Result;
import com.medistock.common.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 供应商接口：分页 / 增删改 */
@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService service;

    @GetMapping
    public Result<PageResult<Supplier>> page(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int pageSize,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer status) {
        // 关键字模糊匹配名称/编码，状态过滤，按 id 倒序（新增排前）
        LambdaQueryWrapper<Supplier> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(Supplier::getName, keyword).or().like(Supplier::getCode, keyword));
        }
        if (status != null) {
            qw.eq(Supplier::getStatus, status);
        }
        qw.orderByDesc(Supplier::getId);
        Page<Supplier> p = service.page(new Page<>(PageUtil.page(page), PageUtil.pageSize(pageSize)), qw);
        return Result.ok(new PageResult<>(p.getTotal(), p.getRecords()));
    }

    /** 详情：存在返回 200，不存在/跨租户返回 404（补齐此前缺失导致 500 的接口） */
    @GetMapping("/{id}")
    public Result<Supplier> detail(@PathVariable Long id) {
        Supplier s = service.getById(id);
        if (s == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "供应商不存在");
        }
        return Result.ok(s);
    }

    @PostMapping
    public Result<Void> create(@RequestBody Supplier s) { service.createSupplier(s); return Result.ok(); }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Supplier s) {
        s.setId(id); service.updateSupplier(s); return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { service.deleteSupplier(id); return Result.ok(); }
}
