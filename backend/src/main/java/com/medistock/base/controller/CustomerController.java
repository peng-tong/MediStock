package com.medistock.base.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medistock.base.entity.Customer;
import com.medistock.base.service.CustomerService;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.common.result.PageResult;
import com.medistock.common.result.Result;
import com.medistock.common.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 客户接口：分页 / 增删改 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    @GetMapping
    public Result<PageResult<Customer>> page(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int pageSize,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer status) {
        // 关键字模糊匹配名称/编码，状态过滤，按 id 倒序（新增排前）
        LambdaQueryWrapper<Customer> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(Customer::getName, keyword).or().like(Customer::getCode, keyword));
        }
        if (status != null) {
            qw.eq(Customer::getStatus, status);
        }
        qw.orderByDesc(Customer::getId);
        Page<Customer> p = service.page(new Page<>(PageUtil.page(page), PageUtil.pageSize(pageSize)), qw);
        return Result.ok(new PageResult<>(p.getTotal(), p.getRecords()));
    }

    /** 详情：存在返回 200，不存在/跨租户返回 404（补齐此前缺失导致 500 的接口） */
    @GetMapping("/{id}")
    public Result<Customer> detail(@PathVariable Long id) {
        Customer c = service.getById(id);
        if (c == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "客户不存在");
        }
        return Result.ok(c);
    }

    @PostMapping
    public Result<Void> create(@RequestBody Customer c) { service.createCustomer(c); return Result.ok(); }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Customer c) {
        c.setId(id); service.updateCustomer(c); return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { service.deleteCustomer(id); return Result.ok(); }
}
