package com.medistock.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;

/** JWT 生成与解析：claims 内置 userId / tenantId / username */
@Slf4j
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMillis;

    public JwtUtil(@Value("${jwt.secret:}") String secret,
                   @Value("${jwt.expire-hours}") long expireHours) {
        // S1 修复：不再信任 application.yml 中硬编码的默认密钥，
        // 未通过 JWT_SECRET 环境变量提供合法密钥时，启动随机生成强密钥，杜绝伪造 token 接管系统
        this.key = resolveKey(secret);
        this.expireMillis = expireHours * 3600_000L;
    }

    /** 配置了满足长度要求的密钥则直接使用；否则生成随机密钥（重启后旧 token 全部失效） */
    private SecretKey resolveKey(String secret) {
        if (secret != null && !secret.isBlank() && secret.length() >= 32) {
            return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        log.warn("jwt.secret 未配置，已自动生成随机密钥（重启后旧 token 全部失效）；生产环境请通过环境变量 JWT_SECRET 指定固定密钥");
        try {
            KeyGenerator kg = KeyGenerator.getInstance("HmacSHA256");
            kg.init(256);
            return kg.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("无法生成 JWT 密钥", e);
        }
    }

    public String createToken(Long userId, Long tenantId, String username) {
        Date now = new Date();
        return Jwts.builder()
                // S5 修复：附加随机 jti，保证同一秒内的两次登录也生成不同 token。
                // 否则登出拉黑 token 后立即重新登录会拿到完全相同的 token，被黑名单误判为已登出（全部接口 401）
                .setId(UUID.randomUUID().toString())
                .setSubject(String.valueOf(userId))
                .claim("tenantId", tenantId)
                .claim("username", username)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expireMillis))
                .signWith(key)
                .compact();
    }

    /** 解析失败（过期/伪造）返回 null */
    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token).getBody();
        } catch (Exception e) {
            return null;
        }
    }
}
