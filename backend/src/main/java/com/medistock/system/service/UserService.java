package com.medistock.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.common.config.TenantContext;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.system.entity.Role;
import com.medistock.system.entity.User;
import com.medistock.system.entity.UserRole;
import com.medistock.system.mapper.RoleMapper;
import com.medistock.system.mapper.UserMapper;
import com.medistock.system.mapper.UserRoleMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 用户管理：增删改查 / 禁用 / 重置密码 / 分配角色 */
@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final com.medistock.system.mapper.TenantMapper tenantMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, UserRoleMapper userRoleMapper,
                       RoleMapper roleMapper, com.medistock.system.mapper.TenantMapper tenantMapper,
                       PasswordEncoder passwordEncoder) {
        // ServiceImpl 在 MP 3.5.7 无带参构造器，直接为 protected baseMapper 赋值
        this.baseMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.tenantMapper = tenantMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /** 新增：校验用户名唯一，密码 BCrypt 加密；非超管禁止通过请求体指定 tenantId（由租户拦截器注入当前租户） */
    public void createUser(User user, String rawPassword) {
        if (!TenantContext.isSuperAdmin()) {
            user.setTenantId(null);
        }
        if (this.count(new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername())) > 0) {
            throw new BusinessException(ErrorCode.USER_EXISTS);
        }
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setStatus(1);
        this.save(user);
    }

    /** 更新：禁止修改 tenant_id（防提权）与 password（防明文落库），updateById 仅更新非 null 字段 */
    public void updateUser(User user) {
        user.setTenantId(null);
        user.setPassword(null);
        updateById(user);
    }

    public Page<User> pageQuery(int page, int pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(User::getUsername, keyword).or().like(User::getRealName, keyword));
        }
        if (status != null) {
            qw.eq(User::getStatus, status);
        }
        qw.orderByDesc(User::getId);
        Page<User> p = this.page(new Page<>(page, pageSize), qw);
        enrich(p.getRecords());
        return p;
    }

    /** 列表展示回填：角色名称列表 / 所属租户名称（批量查询避免 N+1） */
    private void enrich(List<User> users) {
        if (users == null || users.isEmpty()) return;
        List<Long> userIds = users.stream().map(User::getId).toList();
        // 用户 → 角色 id
        java.util.Map<Long, List<Long>> roleIdsByUser = new java.util.HashMap<>();
        userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().in(UserRole::getUserId, userIds))
                .forEach(ur -> roleIdsByUser.computeIfAbsent(ur.getUserId(), k -> new java.util.ArrayList<>())
                        .add(ur.getRoleId()));
        // 角色 id → 角色名
        List<Long> roleIds = roleIdsByUser.values().stream().flatMap(List::stream).distinct().toList();
        java.util.Map<Long, String> roleNames = roleIds.isEmpty() ? java.util.Map.of()
                : roleMapper.selectBatchIds(roleIds).stream()
                        .collect(java.util.stream.Collectors.toMap(Role::getId, Role::getName, (a, b) -> a));
        // 租户 id → 租户名（超管 tenant_id=0 无对应租户，名称为空）
        List<Long> tenantIds = users.stream().map(User::getTenantId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        java.util.Map<Long, String> tenantNames = tenantIds.isEmpty() ? java.util.Map.of()
                : tenantMapper.selectBatchIds(tenantIds).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                com.medistock.system.entity.Tenant::getId,
                                com.medistock.system.entity.Tenant::getName, (a, b) -> a));
        users.forEach(u -> {
            u.setRoles(roleIdsByUser.getOrDefault(u.getId(), List.of()).stream()
                    .map(roleNames::get).filter(java.util.Objects::nonNull).toList());
            u.setTenantName(tenantNames.get(u.getTenantId()));
        });
    }

    public void changeStatus(Long id, Integer status) {
        User user = getRequired(id);
        user.setStatus(status);
        updateById(user);
    }

    /** 重置密码为指定明文（默认 123456） */
    public void resetPassword(Long id, String rawPassword) {
        User user = getRequired(id);
        user.setPassword(passwordEncoder.encode(rawPassword));
        updateById(user);
    }

    /** 分配角色：先删后插；角色必须属于当前租户（非超管时租户拦截器自动过滤，超管可分配任意角色） */
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        getRequired(userId);
        // 校验角色存在且归属当前租户，防止分配越权角色
        if (roleIds != null && !roleIds.isEmpty()) {
            long validCount = roleMapper.selectCount(new LambdaQueryWrapper<Role>().in(Role::getId, roleIds));
            if (validCount != roleIds.size()) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "包含无效角色");
            }
        }
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        for (Long roleId : roleIds) {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            // 超管（tenantId=0）时租户拦截器不注入 tenant_id，而 sys_user_role.tenant_id NOT NULL，
            // 必须显式补默认值（普通租户场景也显式写上当前租户，保证幂等）
            ur.setTenantId(TenantContext.getTenantId() == null ? 0L : TenantContext.getTenantId());
            userRoleMapper.insert(ur);
        }
    }

    /** 删除用户：被角色引用时拒绝，防止产生孤儿关联数据 */
    public void deleteUser(Long id) {
        getRequired(id);
        if (userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id)) > 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "用户已分配角色，请先取消分配");
        }
        removeById(id);
    }

    private User getRequired(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return user;
    }
}
