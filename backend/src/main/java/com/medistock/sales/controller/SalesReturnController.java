package com.medistock.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.common.result.PageResult;
import com.medistock.common.result.Result;
import com.medistock.sales.entity.SalesReturn;
import com.medistock.sales.service.SalesReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 销售退货接口：创建（头+明细，创建即回补库存）/ 分页 / 详情 */
@RestController
@RequestMapping("/api/sales/returns")
@RequiredArgsConstructor
public class SalesReturnController {

    private final SalesReturnService service;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody SalesReturnCreateReq req) {
        return Result.ok(service.createReturn(req.getSalesReturn(), req.getItems()));
    }

    @GetMapping
    public Result<PageResult<SalesReturn>> page(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int pageSize,
                                                @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<SalesReturn> qw = new LambdaQueryWrapper<>();
        if (status != null) qw.eq(SalesReturn::getStatus, status);
        qw.orderByDesc(SalesReturn::getId);
        Page<SalesReturn> p = service.page(new Page<>(page, pageSize), qw);
        return Result.ok(new PageResult<>(p.getTotal(), p.getRecords()));
    }

    @GetMapping("/{id}")
    public Result<SalesReturn> detail(@PathVariable Long id) {
        SalesReturn ret = service.getById(id);
        if (ret == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return Result.ok(ret);
    }
}
