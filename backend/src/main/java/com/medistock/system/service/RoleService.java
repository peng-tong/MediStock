package com.medistock.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.common.config.TenantContext;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.system.entity.Role;
import com.medistock.system.entity.RoleMenu;
import com.medistock.system.entity.UserRole;
import com.medistock.system.mapper.RoleMapper;
import com.medistock.system.mapper.RoleMenuMapper;
import com.medistock.system.mapper.UserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 角色管理 + 角色菜单授权 */
@Service
public class RoleService extends ServiceImpl<RoleMapper, Role> {

    private final RoleMenuMapper roleMenuMapper;
    private final UserRoleMapper userRoleMapper;

    public RoleService(RoleMapper roleMapper, RoleMenuMapper roleMenuMapper, UserRoleMapper userRoleMapper) {
        // ServiceImpl 在 MP 3.5.7 无带参构造器，直接为 protected baseMapper 赋值
        this.baseMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.userRoleMapper = userRoleMapper;
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
        // 第五轮模块F：角色名称/编码必填且限制长度，状态只允许 0/1（防止空角色与脏状态）
        validateRole(role);
        save(role);
    }

    /** 更新：禁止修改 tenant_id（防跨租户篡改），updateById 仅更新非 null 字段 */
    public void updateRole(Role role) {
        role.setTenantId(null);
        // 更新时字段为 null 表示不改，仅对传入字段做校验
        validateRole(role);
        if (role.getId() == null || getById(role.getId()) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        updateById(role);
    }

    /** 角色字段校验：名称/编码 trim 后非空且 ≤64，状态 ∈ {0,1}，备注 ≤255 */
    private void validateRole(Role role) {
        if (role.getName() != null) {
            role.setName(role.getName().trim());
            if (role.getName().isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "角色名称不能为空");
            }
            if (role.getName().length() > 64) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "角色名称长度不能超过 64 个字符");
            }
        }
        if (role.getCode() != null) {
            role.setCode(role.getCode().trim());
            if (role.getCode().isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "角色编码不能为空");
            }
            if (role.getCode().length() > 64) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "角色编码长度不能超过 64 个字符");
            }
        }
        if (role.getStatus() != null && role.getStatus() != 0 && role.getStatus() != 1) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "状态值不合法");
        }
        if (role.getRemark() != null && role.getRemark().length() > 255) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "备注长度不能超过 255 个字符");
        }
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

    /** 删除角色：已被用户或菜单授权引用时拒绝（此前只查 role_menu，漏掉用户引用会产生孤儿关联） */
    public void deleteRole(Long id) {
        if (getById(id) == null) {
            // 与用户删除保持一致：不存在返回 404，而非 200 静默成功
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        if (userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, id)) > 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "角色已分配给用户，请先取消分配");
        }
        if (roleMenuMapper.selectCount(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, id)) > 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "角色已授权菜单，请先取消授权");
        }
        removeById(id);
    }
}
