package com.medistock.common.config;

import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.common.util.JwtUtil;
import com.medistock.common.util.TokenBlacklist;
import com.medistock.system.entity.User;
import com.medistock.system.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证拦截器：
 * 1. 登录/验证码等公开接口放行（见 WebConfig 白名单）
 * 2. 其余接口解析 Authorization: Bearer <token>
 * 3. 将 userId/tenantId 写入 TenantContext，供租户拦截器与业务使用
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;
    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String token = header.substring(7);
        // 登出黑名单校验：token 本身签名/有效期仍合法，但已被登出拉黑时同样按未登录处理（401）
        if (tokenBlacklist.isRevoked(token)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Claims claims = jwtUtil.parseToken(token);
        if (claims == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Long userId;
        try {
            userId = Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            // subject 非法（伪造/损坏），必须拒绝，防止 fail-open 全表放行
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Long tenantId = claims.get("tenantId", Long.class);
        if (tenantId == null) {
            // 缺少 tenantId claim 时拒绝：租户拦截器在上下文为空时按 0（超管）处理会全表放行
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String username = claims.get("username", String.class);

        /*
         * S5 修复：每次请求校验账号是否仍存在且启用，并核对 token 中的 tenantId 与库中用户租户一致。
         * 此前仅验签，用户被禁用/删除后旧 token 在有效期内仍可继续访问（登出黑名单只覆盖主动登出）。
         * 注意：此处尚未写入 TenantContext，租户拦截器按超管放行；查询按主键精确定位后再比对租户，不会越权放行。
         */
        User account = userMapper.selectById(userId);
        if (account == null || account.getStatus() == null || account.getStatus() != 1) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (account.getTenantId() == null || !tenantId.equals(account.getTenantId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 写入请求上下文（UserContext 供业务取当前用户）
        UserContext.setUserId(userId);
        UserContext.setUsername(username);
        TenantContext.setTenantId(tenantId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束清理 ThreadLocal，防止线程池复用串数据
        UserContext.clear();
        TenantContext.clear();
    }
}
