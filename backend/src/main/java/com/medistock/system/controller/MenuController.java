package com.medistock.system.controller;

import com.medistock.common.config.TenantContext;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.common.result.Result;
import com.medistock.system.entity.Menu;
import com.medistock.system.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 菜单管理接口：树形查询 / 增删改 */
@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    /**
     * S5 修复：sys_menu 是平台级共享表（租户拦截器已忽略），前端虽隐藏了菜单管理页面，
     * 但任意登录用户直连接口即可增删改全平台菜单。改为仅平台超管可读写，后端真正拦截而非只靠前端隐藏。
     */
    private void checkSuperAdmin() {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅平台管理员可管理菜单");
        }
    }

    @GetMapping
    public Result<List<Menu>> tree() {
        checkSuperAdmin();
        return Result.ok(menuService.tree());
    }

    @PostMapping
    public Result<Void> create(@RequestBody Menu menu) {
        checkSuperAdmin();
        menuService.createMenu(menu);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Menu menu) {
        checkSuperAdmin();
        menu.setId(id);
        menuService.updateMenu(menu);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        checkSuperAdmin();
        menuService.deleteMenu(id);
        return Result.ok();
    }
}
