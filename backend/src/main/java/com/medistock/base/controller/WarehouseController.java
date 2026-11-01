package com.medistock.base.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medistock.base.entity.Warehouse;
import com.medistock.base.service.WarehouseService;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.common.result.PageResult;
import com.medistock.common.result.Result;
import com.medistock.common.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 仓库接口：分页 / 增删改 */
@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService service;

    @GetMapping
    public Result<PageResult<Warehouse>> page(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int pageSize,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Integer status) {
        // 关键字模糊匹配名称/编码，状态过滤，按 id 倒序（新增排前）
        LambdaQueryWrapper<Warehouse> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(Warehouse::getName, keyword).or().like(Warehouse::getCode, keyword));
        }
        if (status != null) {
            qw.eq(Warehouse::getStatus, status);
        }
        qw.orderByDesc(Warehouse::getId);
        Page<Warehouse> p = service.page(new Page<>(PageUtil.page(page), PageUtil.pageSize(pageSize)), qw);
        return Result.ok(new PageResult<>(p.getTotal(), p.getRecords()));
    }

    /** 详情：存在返回 200，不存在/跨租户返回 404（补齐此前缺失导致 500 的接口） */
    @GetMapping("/{id}")
    public Result<Warehouse> detail(@PathVariable Long id) {
        Warehouse w = service.getById(id);
        if (w == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "仓库不存在");
        }
        return Result.ok(w);
    }

    @PostMapping
    public Result<Void> create(@RequestBody Warehouse w) { service.createWarehouse(w); return Result.ok(); }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Warehouse w) {
        w.setId(id); service.updateWarehouse(w); return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { service.deleteWarehouse(id); return Result.ok(); }
}
