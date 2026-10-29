package com.medistock.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medistock.common.result.PageResult;
import com.medistock.common.result.Result;
import com.medistock.purchase.entity.PurchaseOrder;
import com.medistock.purchase.service.InboundReq;
import com.medistock.purchase.service.PurchaseInboundService;
import com.medistock.purchase.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 采购单接口：创建（头+明细）/ 分页 / 详情 / 删除（仅草稿）/ 入库 */
@RestController
@RequestMapping("/api/purchases/orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService service;
    private final PurchaseInboundService inboundService;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody PurchaseOrderCreateReq req) {
        return Result.ok(service.createOrder(req.getOrder(), req.getItems()));
    }

    @GetMapping
    public Result<PageResult<PurchaseOrder>> page(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int pageSize,
                                                  @RequestParam(required = false) String orderNo,
                                                  @RequestParam(required = false) Integer status) {
        Page<PurchaseOrder> p = service.pageQuery(page, pageSize, orderNo, status);
        return Result.ok(new PageResult<>(p.getTotal(), p.getRecords()));
    }

    @GetMapping("/{id}")
    public Result<PurchaseOrder> detail(@PathVariable Long id) {
        return Result.ok(service.detail(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.deleteOrder(id);
        return Result.ok();
    }

    @PostMapping("/{id}/inbound")
    public Result<Void> inbound(@PathVariable Long id, @RequestBody InboundReq req) {
        inboundService.inbound(id, req.getItems());
        return Result.ok();
    }
}
