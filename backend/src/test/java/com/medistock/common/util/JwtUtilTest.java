package com.medistock.common.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    /**
     * 测试密钥：HS256 要求密钥长度 ≥ 32 字节（256 bit），
     * 计划原文的 "test-secret-key-1234567890" 仅 26 字符，会导致 Keys.hmacShaKeyFor 抛异常，
     * 故补足到 35 字符，用例断言不变。
     */
    private static final String TEST_SECRET = "test-secret-key-1234567890-abcdefgh";

    @Test
    void token_roundTrip() {
        JwtUtil util = new JwtUtil(TEST_SECRET, 1);
        String token = util.createToken(1L, 2L, "admin");
        Claims claims = util.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("tenantId", Long.class)).isEqualTo(2L);
        assertThat(claims.get("username", String.class)).isEqualTo("admin");
    }

    @Test
    void invalidToken_returnsNull() {
        JwtUtil util = new JwtUtil(TEST_SECRET, 1);
        assertThat(util.parseToken("not-a-token")).isNull();
    }
}
