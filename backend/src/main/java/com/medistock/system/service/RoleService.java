package com.medistock.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.common.config.TenantContext;
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

    /** 新增：非超管强制当前租户，超管未传时默认 0（sys_role.tenant_id NOT NULL） */
    public void createRole(Role role) {
        if (!TenantContext.isSuperAdmin()) {
            role.setTenantId(null);
        }
        if (role.getTenantId() == null) {
            role.setTenantId(TenantContext.getTenantId() == null ? 0L : TenantContext.getTenantId());
        }
        save(role);
    }

    /** 更新：禁止修改 tenant_id（防跨租户篡改），updateById 仅更新非 null 字段 */
    public void updateRole(Role role) {
        role.setTenantId(null);
        updateById(role);
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
            // 超管（tenantId=0）时租户拦截器不注入 tenant_id，而 sys_role_menu.tenant_id NOT NULL，
            // 必须显式补默认值（普通租户场景也显式写上当前租户，保证幂等）
            rm.setTenantId(TenantContext.getTenantId() == null ? 0L : TenantContext.getTenantId());
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
