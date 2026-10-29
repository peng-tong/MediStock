package com.medistock.system.controller;

import com.medistock.common.result.Result;
import com.medistock.system.entity.Role;
import com.medistock.system.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 角色管理接口：列表 / 增删改 / 菜单授权 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public Result<List<Role>> list() { return Result.ok(roleService.listAll()); }

    @PostMapping
    public Result<Void> create(@RequestBody Role role) { roleService.createRole(role); return Result.ok(); }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Role role) {
        role.setId(id); roleService.updateRole(role); return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { roleService.deleteRole(id); return Result.ok(); }

    @PutMapping("/{id}/menus")
    public Result<Void> grantMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.grantMenus(id, menuIds); return Result.ok();
    }
}
