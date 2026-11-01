package com.medistock.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medistock.common.result.PageResult;
import com.medistock.common.result.Result;
import com.medistock.common.util.PageUtil;
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

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PurchaseOrderCreateReq req) {
        service.updateOrder(id, req.getOrder(), req.getItems());
        return Result.ok();
    }

    @GetMapping
    public Result<PageResult<PurchaseOrder>> page(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int pageSize,
                                                  @RequestParam(required = false) String orderNo,
                                                  // 逗号分隔多状态：如 "1,2" 表示待审核+待入库（采购入库页按多状态过滤）
                                                  @RequestParam(required = false) String status) {
        Page<PurchaseOrder> p = service.pageQuery(PageUtil.page(page), PageUtil.pageSize(pageSize), orderNo, status);
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

    @PostMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable Long id) {
        service.submitOrder(id);
        return Result.ok();
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        service.approveOrder(id);
        return Result.ok();
    }

    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id) {
        service.rejectOrder(id);
        return Result.ok();
    }

    @PostMapping("/{id}/inbound")
    public Result<Void> inbound(@PathVariable Long id, @RequestBody InboundReq req) {
        // S6 body 可能为 null（空请求体），此时按空明细处理由 service 统一返回 422
        inboundService.inbound(id, req == null ? null : req.getItems());
        return Result.ok();
    }
}
