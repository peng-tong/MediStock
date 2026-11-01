package com.medistock.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medistock.common.config.TenantContext;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.common.result.PageResult;
import com.medistock.common.result.Result;
import com.medistock.system.entity.Tenant;
import com.medistock.system.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 租户管理接口（仅平台超管可访问，普通租户越权直接拒绝） */
@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    /** 越权拦截：租户列表含联系人手机号等敏感信息，非平台超管一律禁止 */
    private void checkSuperAdmin() {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅平台管理员可操作租户");
        }
    }

    @GetMapping
    public Result<PageResult<Tenant>> page(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int pageSize,
                                           @RequestParam(required = false) String keyword) {
        checkSuperAdmin();
        Page<Tenant> p = tenantService.pageQuery(page, pageSize, keyword);
        return Result.ok(new PageResult<>(p.getTotal(), p.getRecords()));
    }

    @PostMapping
    public Result<Void> create(@RequestBody Tenant tenant) {
        checkSuperAdmin();
        tenantService.save(tenant);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Tenant tenant) {
        checkSuperAdmin();
        tenant.setId(id);
        tenantService.updateById(tenant);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        checkSuperAdmin();
        tenantService.changeStatus(id, status);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        checkSuperAdmin();
        tenantService.deleteTenant(id);
        return Result.ok();
    }
}
