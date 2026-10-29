package com.medistock.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.system.entity.Role;
import com.medistock.system.entity.RoleMenu;
import com.medistock.system.mapper.RoleMapper;
import com.medistock.system.mapper.RoleMenuMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 角色管理 + 角色菜单授权 */
@Service
public class RoleService extends ServiceImpl<RoleMapper, Role> {

    private final RoleMenuMapper roleMenuMapper;

    public RoleService(RoleMapper roleMapper, RoleMenuMapper roleMenuMapper) {
        // ServiceImpl 在 MP 3.5.7 无带参构造器，直接为 protected baseMapper 赋值
        this.baseMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    public List<Role> listAll() {
        return this.list(new LambdaQueryWrapper<Role>().orderByAsc(Role::getId));
    }

    /** 保存角色-菜单授权：先删后插，事务保证原子 */
    @Transactional
    public void grantMenus(Long roleId, List<Long> menuIds) {
        Role role = getById(roleId);
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId));
        for (Long menuId : menuIds) {
            RoleMenu rm = new RoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
    }

    /** 删除角色：被用户引用时拒绝 */
    public void deleteRole(Long id) {
        if (roleMenuMapper.selectCount(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, id)) > 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "角色已授权菜单，请先取消授权");
        }
        removeById(id);
    }
}
