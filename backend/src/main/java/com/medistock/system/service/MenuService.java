package com.medistock.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.system.entity.Menu;
import com.medistock.system.mapper.MenuMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 菜单管理：树形结构，前端按权限动态渲染的依据 */
@Service
public class MenuService extends ServiceImpl<MenuMapper, Menu> {

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

    public void deleteMenu(Long id) {
        if (this.count(new LambdaQueryWrapper<Menu>().eq(Menu::getParentId, id)) > 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "存在子菜单，禁止删除");
        }
        removeById(id);
    }
}
