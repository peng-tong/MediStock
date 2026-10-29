package com.medistock.system.controller;

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

    @GetMapping
    public Result<List<Menu>> tree() {
        return Result.ok(menuService.tree());
    }

    @PostMapping
    public Result<Void> create(@RequestBody Menu menu) {
        menuService.save(menu);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Menu menu) {
        menu.setId(id);
        menuService.updateById(menu);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return Result.ok();
    }
}
