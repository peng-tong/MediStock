package com.medistock.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.system.entity.User;
import com.medistock.system.entity.UserRole;
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
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, UserRoleMapper userRoleMapper, PasswordEncoder passwordEncoder) {
        // ServiceImpl 在 MP 3.5.7 无带参构造器，直接为 protected baseMapper 赋值
        this.baseMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /** 新增：校验用户名唯一，密码 BCrypt 加密 */
    public void createUser(User user, String rawPassword) {
        if (this.count(new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername())) > 0) {
            throw new BusinessException(ErrorCode.USER_EXISTS);
        }
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setStatus(1);
        this.save(user);
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
        return this.page(new Page<>(page, pageSize), qw);
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

    /** 分配角色：先删后插 */
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        getRequired(userId);
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        for (Long roleId : roleIds) {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }

    private User getRequired(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return user;
    }
}
