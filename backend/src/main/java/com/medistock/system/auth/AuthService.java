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
        //    此处必须显式 select 密码列才能取到密文
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .select(User::getId, User::getUsername, User::getPassword,
                        User::getStatus, User::getTenantId, User::getRealName)
                .eq(User::getUsername, req.getUsername()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        // 3. 非超管时校验租户状态
        if (user.getTenantId() != 0L) {
            Tenant tenant = tenantMapper.selectById(user.getTenantId());
            if (tenant == null || tenant.getStatus() == 0) {
                throw new BusinessException(ErrorCode.TENANT_DISABLED);
            }
        }
        // 4. 更新最后登录时间 + 签发 token
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        String token = jwtUtil.createToken(user.getId(), user.getTenantId(), user.getUsername());
        return Map.of("token", token, "username", user.getUsername(),
                "realName", user.getRealName() == null ? user.getUsername() : user.getRealName());
    }

    public void changePassword(ChangePasswordReq req) {
        Long userId = UserContext.getUserId();
        // 显式 select 密码列（实体默认 select=false）
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .select(User::getId, User::getPassword)
                .eq(User::getId, userId));
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.VALIDATE_ERROR, "原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userMapper.updateById(user);
    }
}
