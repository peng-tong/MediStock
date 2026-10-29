package com.medistock.common.config;

import com.medistock.common.exception.BusinessException;
import com.medistock.common.exception.ErrorCode;
import com.medistock.common.util.JwtUtil;
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

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Claims claims = jwtUtil.parseToken(header.substring(7));
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
