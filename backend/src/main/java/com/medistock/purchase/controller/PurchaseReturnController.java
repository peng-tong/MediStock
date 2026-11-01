package com.medistock.purchase.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.common.result.PageResult;
import com.medistock.common.result.Result;
import com.medistock.common.util.PageUtil;
import com.medistock.purchase.entity.PurchaseReturn;
import com.medistock.purchase.service.PurchaseReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 采购退货接口：创建（头+明细，创建即冲减库存）/ 分页 / 详情 */
@RestController
@RequestMapping("/api/purchases/returns")
@RequiredArgsConstructor
public class PurchaseReturnController {

    private final PurchaseReturnService service;
    private final com.medistock.common.service.NameResolver nameResolver;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody PurchaseReturnCreateReq req) {
        return Result.ok(service.createReturn(req.getPurchaseReturn(), req.getItems()));
    }

    @GetMapping
    public Result<PageResult<PurchaseReturn>> page(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int pageSize,
                                                   @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<PurchaseReturn> qw = new LambdaQueryWrapper<>();
        if (status != null) qw.eq(PurchaseReturn::getStatus, status);
        qw.orderByDesc(PurchaseReturn::getId);
        Page<PurchaseReturn> p = service.page(new Page<>(PageUtil.page(page), PageUtil.pageSize(pageSize)), qw);
        // 列表回填供应商名称（一次批量查询）
        java.util.Map<Long, String> suppliers = nameResolver.supplierNames(
                p.getRecords().stream().map(PurchaseReturn::getSupplierId).toList());
        p.getRecords().forEach(r -> r.setSupplierName(suppliers.get(r.getSupplierId())));
        return Result.ok(new PageResult<>(p.getTotal(), p.getRecords()));
    }

    @GetMapping("/{id}")
    public Result<PurchaseReturn> detail(@PathVariable Long id) {
        PurchaseReturn ret = service.getById(id);
        if (ret == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return Result.ok(ret);
    }
}
