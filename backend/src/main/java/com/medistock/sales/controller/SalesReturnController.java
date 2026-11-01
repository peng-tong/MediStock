package com.medistock.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.common.result.PageResult;
import com.medistock.common.result.Result;
import com.medistock.common.util.PageUtil;
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
    private final com.medistock.common.service.NameResolver nameResolver;

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
        Page<SalesReturn> p = service.page(new Page<>(PageUtil.page(page), PageUtil.pageSize(pageSize)), qw);
        // 列表回填客户名称（一次批量查询）
        java.util.Map<Long, String> customers = nameResolver.customerNames(
                p.getRecords().stream().map(SalesReturn::getCustomerId).toList());
        p.getRecords().forEach(r -> r.setCustomerName(customers.get(r.getCustomerId())));
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
