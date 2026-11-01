package com.medistock.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.common.config.TenantContext;
import com.medistock.common.config.UserContext;
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

    /*
     * 已知限制（测试问题 73，暂不处理）：
     * 系统角色/权限（RBAC）尚未完善，任意已登录用户均可增删改同租户内的用户，
     * 当前将其视为同租户内的正常管理行为，不做接口级权限校验；
     * 待「角色-菜单-接口权限」体系落地后再统一收紧（需返回 403）。
     */

    /** 新增：校验用户名唯一，密码 BCrypt 加密；非超管强制绑定当前租户，超管必须显式指定归属租户 */
    public void createUser(User user, String rawPassword) {
        if (TenantContext.isSuperAdmin()) {
            // S2 修复：超管创建用户必须显式指定归属租户，防止 tenantId=null 被租户拦截器默认 0 落库，
            // 变成平台级（超管）用户，导致身份提权
            if (user.getTenantId() == null || user.getTenantId() <= 0) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "创建用户必须指定归属租户");
            }
            if (tenantMapper.selectById(user.getTenantId()) == null) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "指定租户不存在");
            }
        } else {
            // 普通用户：强制绑定当前租户，忽略请求体中的 tenantId（防越权跨租户创建）
            user.setTenantId(TenantContext.getTenantId());
        }
        // 第五轮模块F：服务端统一做字段规范化与合法性校验，防止绕过前端直接提交空/空格/超长字段
        normalizeAndValidate(user, false);
        validatePassword(rawPassword);
        // 保存时使用规范化后的用户名查重
        // S5 修复：查重需包含已逻辑删除的同名账号，否则插入撞唯一索引直接 500
        if (baseMapper.countUsernameIncludeDeleted(user.getUsername()) > 0) {
            throw new BusinessException(ErrorCode.USER_EXISTS, "用户名已存在（可能被已删除账号占用）");
        }
        user.setPassword(passwordEncoder.encode(rawPassword));
        // 前端表单允许选择启用/禁用，此前无条件写 1 导致新建禁用用户实际可用；改为合法值原样保存
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        this.save(user);
    }

    /** 更新：禁止修改 tenant_id（防提权）与 password（防明文落库），updateById 仅更新非 null 字段 */
    public void updateUser(User user) {
        user.setTenantId(null);
        user.setPassword(null);
        // 第五轮模块F：编辑同样校验字段（用户名/姓名若传空或纯空格必须拒绝，避免列表出现空白账号）
        normalizeAndValidate(user, true);
        // 状态只允许 0/1，防止直调接口写入 status=2 等非法值造成「既非启用也非禁用」的脏状态
        if (user.getStatus() != null && user.getStatus() != 0 && user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "状态值不合法");
        }
        // 改名时同样要避开已存在的用户名（含逻辑删除记录），否则会撞唯一索引返回 500
        if (user.getUsername() != null
                && baseMapper.countUsernameIncludeDeletedExcept(user.getUsername(), user.getId()) > 0) {
            throw new BusinessException(ErrorCode.USER_EXISTS, "用户名已存在（可能被已删除账号占用）");
        }
        boolean updated = updateById(user);
        if (!updated) {
            // S3 修复：跨租户/不存在的用户被租户拦截器过滤后命中 0 行，此前伪装 200 ok，改为明确 404
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在或无权限操作");
        }
    }

    /**
     * 字段规范化 + 合法性校验（新增/编辑共用）：
     * - 用户名/姓名 trim 后不能为空，长度不超过表字段上限（64）
     * - 手机/邮箱为选填，超长直接拒绝，避免 SQLite 静默截断或前端溢出
     * - 更新时字段为 null 表示不改，不做必填校验
     */
    private void normalizeAndValidate(User user, boolean update) {
        if (user.getUsername() != null) {
            user.setUsername(user.getUsername().trim());
            if (user.getUsername().isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "用户名不能为空");
            }
            if (user.getUsername().length() > 64) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "用户名长度不能超过 64 个字符");
            }
        } else if (!update) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "用户名不能为空");
        }
        if (user.getRealName() != null) {
            user.setRealName(user.getRealName().trim());
            if (user.getRealName().isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "姓名不能为空");
            }
            if (user.getRealName().length() > 64) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "姓名长度不能超过 64 个字符");
            }
        } else if (!update) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "姓名不能为空");
        }
        if (user.getPhone() != null) {
            user.setPhone(user.getPhone().trim());
            if (user.getPhone().length() > 32) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "手机号长度不能超过 32 个字符");
            }
        }
        if (user.getEmail() != null) {
            user.setEmail(user.getEmail().trim());
            if (user.getEmail().length() > 64) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "邮箱长度不能超过 64 个字符");
            }
            // 基础格式校验：有值时必须包含 @ 与域名点号（空字符串视为未填写）
            if (!user.getEmail().isEmpty() && !user.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "邮箱格式不正确");
            }
        }
        // 新增同样校验状态：此前 create 无条件写 1，非法值被静默吞掉，合法禁用值也被覆盖
        if (user.getStatus() != null && user.getStatus() != 0 && user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "状态值不合法");
        }
    }

    /** 密码强度下限：新增/重置密码均要求 6~64 位（避免空密码、1 位弱密码与超长 BCrypt 截断风险） */
    private void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "密码不能为空");
        }
        if (rawPassword.length() < 6 || rawPassword.length() > 64) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "密码长度必须为 6~64 位");
        }
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
        // S5 修复：禁用/启用接口同样只接受 0/1，避免非法状态值落库
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "状态值不合法");
        }
        // 第五轮模块F：禁止禁用当前登录账号（否则把自己踢下线且无法再操作）
        if (status == 0 && id.equals(UserContext.getUserId())) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "不能禁用当前登录账号");
        }
        User user = getRequired(id);
        // 禁止禁用租户内最后一个启用账号，避免整个租户无人可登录
        if (status == 0 && user.getStatus() != null && user.getStatus() == 1) {
            long otherEnabled = this.count(new LambdaQueryWrapper<User>()
                    .eq(User::getStatus, 1).ne(User::getId, id));
            if (otherEnabled == 0) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "不能禁用租户内最后一个启用账号");
            }
        }
        user.setStatus(status);
        updateById(user);
    }

    /** 重置密码为指定明文（默认 123456） */
    public void resetPassword(Long id, String rawPassword) {
        // 第五轮模块F：重置密码同样执行强度校验，防止 API 直调把他人密码重置为 1 位弱密码
        validatePassword(rawPassword);
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
        // 第五轮模块F：禁止删除当前登录账号（此前允许自删，删除后无法登录且列表不可见）
        if (id.equals(UserContext.getUserId())) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "不能删除当前登录账号");
        }
        User user = getRequired(id);
        if (userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id)) > 0) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "用户已分配角色，请先取消分配");
        }
        // 禁止删除租户内最后一个启用账号，避免租户失去唯一可登录账号
        if (user.getStatus() != null && user.getStatus() == 1) {
            long otherEnabled = this.count(new LambdaQueryWrapper<User>()
                    .eq(User::getStatus, 1).ne(User::getId, id));
            if (otherEnabled == 0) {
                throw new BusinessException(ErrorCode.VALIDATE_ERROR, "不能删除租户内最后一个启用账号");
            }
        }
        removeById(id);
    }

    /** 详情：用户不存在/跨租户返回 404（补齐此前缺失的 GET /api/users/{id} 所依赖的查询） */
    public User detail(Long id) {
        User user = getRequired(id);
        // 详情与列表保持一致：同样回填角色名/租户名（此前详情返回 null，与列表对不上）
        enrich(List.of(user));
        return user;
    }

    private User getRequired(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return user;
    }
}
