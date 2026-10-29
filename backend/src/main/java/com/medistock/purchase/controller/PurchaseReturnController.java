package com.medistock.purchase.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medistock.common.result.PageResult;
import com.medistock.common.result.Result;
import com.medistock.purchase.entity.PurchaseReturn;
import com.medistock.purchase.service.PurchaseReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 采购退货接口：创建（头+明细，创建即冲减库存）/ 分页 / 详情 */
@RestController
@RequestMapping("/api/purchases/returns")
@RequiredArgsConstructor
public class PurchaseReturnController {

    private final PurchaseReturnService service;

    @PostMapping
    public Result<Long> create(@RequestBody PurchaseReturnCreateReq req) {
        return Result.ok(service.createReturn(req.getOrder(), req.getItems()));
    }

    @GetMapping
    public Result<PageResult<PurchaseReturn>> page(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int pageSize,
                                                   @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<PurchaseReturn> qw = new LambdaQueryWrapper<>();
        if (status != null) qw.eq(PurchaseReturn::getStatus, status);
        qw.orderByDesc(PurchaseReturn::getId);
        Page<PurchaseReturn> p = service.page(new Page<>(page, pageSize), qw);
        return Result.ok(new PageResult<>(p.getTotal(), p.getRecords()));
    }

    @GetMapping("/{id}")
    public Result<PurchaseReturn> detail(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }
}
