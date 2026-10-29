package com.medistock.system.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medistock.common.config.UserContext;
import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.common.util.JwtUtil;
import com.medistock.system.entity.Tenant;
import com.medistock.system.entity.User;
import com.medistock.system.mapper.TenantMapper;
import com.medistock.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 认证服务：验证码 → 用户名密码 → 签发 JWT */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final CaptchaStore captchaStore;
    private final UserMapper userMapper;
    private final TenantMapper tenantMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public Map<String, Object> login(LoginReq req) {
        // 1. 校验图形验证码
        if (!captchaStore.verify(req.getCaptchaKey(), req.getCaptchaCode())) {
            throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
        }
        // 2. 校验账号密码（BCrypt）——实体 password 默认 select=false，
        //    此处必须显式 select 密码列才能取到密文。
        //    /api/auth/login 是公开接口（无租户上下文），租户拦截器全表放行，
        //    同一 username 可能属于多个租户（唯一索引为 tenant_id+username 联合），
        //    因此用 selectList 全库匹配，逐个校验密码，任一命中即登录成功。
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .select(User::getId, User::getUsername, User::getPassword,
                        User::getStatus, User::getTenantId, User::getRealName)
                .eq(User::getUsername, req.getUsername()));
        User matched = null;
        for (User u : users) {
            if (passwordEncoder.matches(req.getPassword(), u.getPassword())) {
                matched = u;
                break;
            }
        }
        if (matched == null) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        if (matched.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        // 3. 非超管时校验租户状态
        if (matched.getTenantId() != 0L) {
            Tenant tenant = tenantMapper.selectById(matched.getTenantId());
            if (tenant == null || tenant.getStatus() == 0) {
                throw new BusinessException(ErrorCode.TENANT_DISABLED);
            }
        }
        // 4. 更新最后登录时间 + 签发 token
        matched.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(matched);

        String token = jwtUtil.createToken(matched.getId(), matched.getTenantId(), matched.getUsername());
        return Map.of("token", token, "username", matched.getUsername(),
                "realName", matched.getRealName() == null ? matched.getUsername() : matched.getRealName());
    }

    public void changePassword(ChangePasswordReq req) {
        Long userId = UserContext.getUserId();
        // 显式 select 密码列（实体默认 select=false）
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .select(User::getId, User::getPassword)
                .eq(User::getId, userId));
        if (user == null) {
            // 用户被逻辑删除等场景查不到，直接 404，避免空指针
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userMapper.updateById(user);
    }
}
