package com.medistock.base.controller;

import com.medistock.base.entity.Customer;
import com.medistock.base.service.CustomerService;
import com.medistock.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 客户接口：列表 / 增删改 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    @GetMapping
    public Result<List<Customer>> list() { return Result.ok(service.list()); }

    @PostMapping
    public Result<Void> create(@RequestBody Customer c) { service.createCustomer(c); return Result.ok(); }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Customer c) {
        c.setId(id); service.updateCustomer(c); return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { service.removeById(id); return Result.ok(); }
}
