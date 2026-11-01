package com.medistock.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medistock.common.config.TenantContext;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.common.result.PageResult;
import com.medistock.common.result.Result;
import com.medistock.system.entity.Role;
import com.medistock.system.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 角色管理接口：分页 / 增删改 / 菜单授权 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 第五轮模块F：角色管理属平台超管功能（前端已屏蔽角色页），
     * 此前普通租户账号可直连接口增改删角色，现统一返回 403。
     */
    private void checkSuperAdmin() {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅平台管理员可管理角色");
        }
    }

    @GetMapping
    public Result<PageResult<Role>> page(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int pageSize,
                                         @RequestParam(required = false) String keyword) {
        checkSuperAdmin();
        // 关键字模糊匹配角色名，按 id 升序
        LambdaQueryWrapper<Role> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.like(Role::getName, keyword);
        }
        qw.orderByAsc(Role::getId);
        Page<Role> p = roleService.page(new Page<>(page, pageSize), qw);
        return Result.ok(new PageResult<>(p.getTotal(), p.getRecords()));
    }

    @PostMapping
    public Result<Void> create(@RequestBody Role role) { checkSuperAdmin(); roleService.createRole(role); return Result.ok(); }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Role role) {
        checkSuperAdmin();
        role.setId(id); roleService.updateRole(role); return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { checkSuperAdmin(); roleService.deleteRole(id); return Result.ok(); }

    @PutMapping("/{id}/menus")
    public Result<Void> grantMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        checkSuperAdmin();
        roleService.grantMenus(id, menuIds); return Result.ok();
    }
}
