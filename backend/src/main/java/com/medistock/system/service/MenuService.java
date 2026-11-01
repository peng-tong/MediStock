package com.medistock.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.system.entity.Menu;
import com.medistock.system.entity.RoleMenu;
import com.medistock.system.mapper.MenuMapper;
import com.medistock.system.mapper.RoleMenuMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 菜单管理：树形结构，前端按权限动态渲染的依据 */
@Service
public class MenuService extends ServiceImpl<MenuMapper, Menu> {

    private final RoleMenuMapper roleMenuMapper;

    public MenuService(MenuMapper menuMapper, RoleMenuMapper roleMenuMapper) {
        // ServiceImpl 在 MP 3.5.7 无带参构造器，直接为 protected baseMapper 赋值
        this.baseMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    /** 返回整棵菜单树 */
    public List<Menu> tree() {
        List<Menu> all = this.list(new LambdaQueryWrapper<Menu>().orderByAsc(Menu::getSort));
        Map<Long, List<Menu>> byParent = all.stream()
                .collect(Collectors.groupingBy(Menu::getParentId));
        return buildTree(0L, byParent);
    }

    private List<Menu> buildTree(Long parentId, Map<Long, List<Menu>> byParent) {
        List<Menu> children = new ArrayList<>();
        for (Menu m : byParent.getOrDefault(parentId, List.of())) {
            m.setChildren(buildTree(m.getId(), byParent));
            children.add(m);
        }
        return children;
    }

    /**
     * 新增菜单：名称必填且 ≤64，类型 1~3，状态 0/1，父菜单必须存在（防止悬空 parentId 导致树丢失）
     */
    public void createMenu(Menu menu) {
        validateMenu(menu, true);
        if (menu.getParentId() != null && menu.getParentId() != 0 && getById(menu.getParentId()) == null) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "父菜单不存在");
        }
        save(menu);
    }

    /**
     * 更新菜单：除字段校验外，禁止把父级指向自身（会造成树构建无限递归/500）
     */
    public void updateMenu(Menu menu) {
        if (menu.getId() == null || getById(menu.getId()) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "菜单不存在");
        }
        validateMenu(menu, false);
        if (menu.getParentId() != null && menu.getParentId() == menu.getId()) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "父菜单不能指向自身");
        }
        if (menu.getParentId() != null && menu.getParentId() != 0 && getById(menu.getParentId()) == null) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "父菜单不存在");
        }
        updateById(menu);
    }

    /** 菜单字段校验：更新时 null 表示不改 */
    private void validateMenu(Menu menu, boolean create) {
        if (menu.getName() != null) {
            menu.setName(menu.getName().trim());
            if (menu.getName().isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "菜单名称不能为空");
            }
            if (menu.getName().length() > 64) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "菜单名称长度不能超过 64 个字符");
            }
        } else if (create) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "菜单名称不能为空");
        }
        if (menu.getType() != null && (menu.getType() < 1 || menu.getType() > 3)) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "菜单类型不合法");
        }
        if (menu.getStatus() != null && menu.getStatus() != 0 && menu.getStatus() != 1) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "状态值不合法");
        }
    }

    /**
     * 删除菜单：存在子菜单或仍被角色授权引用时拒绝。
     * 此前只校验子菜单，角色-菜单关联会变成孤儿数据（角色权限与菜单实际配置不一致）。
     */
    public void deleteMenu(Long id) {
        if (getById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "菜单不存在");
        }
        if (this.count(new LambdaQueryWrapper<Menu>().eq(Menu::getParentId, id)) > 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "存在子菜单，禁止删除");
        }
        if (roleMenuMapper.selectCount(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getMenuId, id)) > 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "菜单已被角色引用，请先取消授权");
        }
        removeById(id);
    }
}
