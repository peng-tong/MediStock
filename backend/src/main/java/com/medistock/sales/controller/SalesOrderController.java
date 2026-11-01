package com.medistock.sales.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medistock.common.result.PageResult;
import com.medistock.common.result.Result;
import com.medistock.common.util.PageUtil;
import com.medistock.sales.entity.SalesOrder;
import com.medistock.sales.service.SalesOrderService;
import com.medistock.sales.service.SalesOutboundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 销售单接口：创建（头+明细）/ 分页 / 详情 / 删除（仅草稿）/ 出库 */
@RestController
@RequestMapping("/api/sales/orders")
@RequiredArgsConstructor
public class SalesOrderController {

    private final SalesOrderService service;
    private final SalesOutboundService outboundService;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody SalesOrderCreateReq req) {
        return Result.ok(service.createOrder(req.getOrder(), req.getItems()));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SalesOrderCreateReq req) {
        service.updateOrder(id, req.getOrder(), req.getItems());
        return Result.ok();
    }

    @GetMapping
    public Result<PageResult<SalesOrder>> page(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int pageSize,
                                               @RequestParam(required = false) String orderNo,
                                               @RequestParam(required = false) Integer status) {
        Page<SalesOrder> p = service.pageQuery(PageUtil.page(page), PageUtil.pageSize(pageSize), orderNo, status);
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

    @PostMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable Long id) {
        service.submitOrder(id);
        return Result.ok();
    }

    @PostMapping("/{id}/outbound")
    public Result<Void> outbound(@PathVariable Long id) {
        outboundService.outbound(id);
        return Result.ok();
    }
}
