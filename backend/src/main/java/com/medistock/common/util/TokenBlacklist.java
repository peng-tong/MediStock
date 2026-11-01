package com.medistock.common.util;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT 登出黑名单（单机内存实现）：
 * JWT 本身无状态，登出后 token 仍会一直有效到过期；这里把已登出的 token 记录到内存，
 * 由 AuthInterceptor 每次请求校验，命中即返回 401，从而补齐「登出即失效」的语义。
 *
 * 说明：仅在单实例内有效（多实例部署需换成 Redis 等共享存储）；记录会在 token 自然过期后清理，
 * 不会无限增长。
 */
@Component
public class TokenBlacklist {

    /** token -> 该 token 的过期时间戳（毫秒），过期后自动移除 */
    private final Map<String, Long> revoked = new ConcurrentHashMap<>();

    /** 拉黑一个 token（expireAtMillis 取自 JWT 的 expiration） */
    public void revoke(String token, long expireAtMillis) {
        if (token == null || token.isBlank()) {
            return;
        }
        revoked.put(token, expireAtMillis);
    }

    /** 判断 token 是否已登出（顺带清理已自然过期的记录） */
    public boolean isRevoked(String token) {
        if (token == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        revoked.entrySet().removeIf(e -> e.getValue() <= now);
        return revoked.containsKey(token);
    }
}
