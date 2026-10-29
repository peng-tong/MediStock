package com.medistock.sales.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medistock.common.result.PageResult;
import com.medistock.common.result.Result;
import com.medistock.sales.entity.SalesOrder;
import com.medistock.sales.service.SalesOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 销售单接口：创建（头+明细）/ 分页 / 详情 / 删除（仅草稿） */
@RestController
@RequestMapping("/api/sales/orders")
@RequiredArgsConstructor
public class SalesOrderController {

    private final SalesOrderService service;

    @PostMapping
    public Result<Long> create(@RequestBody SalesOrderCreateReq req) {
        return Result.ok(service.createOrder(req.getOrder(), req.getItems()));
    }

    @GetMapping
    public Result<PageResult<SalesOrder>> page(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int pageSize,
                                               @RequestParam(required = false) String orderNo,
                                               @RequestParam(required = false) Integer status) {
        Page<SalesOrder> p = service.pageQuery(page, pageSize, orderNo, status);
        return Result.ok(new PageResult<>(p.getTotal(), p.getRecords()));
    }

    @GetMapping("/{id}")
    public Result<SalesOrder> detail(@PathVariable Long id) {
        return Result.ok(service.detail(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.deleteOrder(id);
        return Result.ok();
    }
}
