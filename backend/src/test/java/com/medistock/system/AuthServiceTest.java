package com.medistock.system;

import com.medistock.common.config.TenantContext;
import com.medistock.common.exception.BusinessException;
import com.medistock.system.auth.AuthService;
import com.medistock.system.auth.CaptchaStore;
import com.medistock.system.auth.LoginReq;
import com.medistock.system.entity.Tenant;
import com.medistock.system.entity.User;
import com.medistock.system.mapper.TenantMapper;
import com.medistock.system.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private CaptchaStore captchaStore;
    @Autowired
    private UserService userService;
    @Autowired
    private TenantMapper tenantMapper;

    @Test
    void login_success_withValidCaptcha() {
        TenantContext.setTenantId(1L);
        try {
            // 测试库无种子数据，需自建一个启用状态的租户（登录会校验租户状态）
            Tenant tenant = new Tenant();
            tenant.setName("测试租户");
            tenant.setCode("t" + System.currentTimeMillis());
            tenant.setStatus(1);
            tenant.setMaxUserCount(10);
            tenantMapper.insert(tenant);

            // 造一个属于该租户的用户
            User u = new User();
            u.setTenantId(tenant.getId());
            u.setUsername("auth_" + System.currentTimeMillis());
            userService.createUser(u, "pass123456");

            // 预置验证码
            captchaStore.put("key1", "abcd");

            LoginReq req = new LoginReq();
            req.setUsername(u.getUsername());
            req.setPassword("pass123456");
            req.setCaptchaKey("key1");
            req.setCaptchaCode("abcd");

            Map<String, Object> result = authService.login(req);
            assertThat(result.get("token")).isNotNull();
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void login_fails_withWrongCaptcha() {
        captchaStore.put("key2", "abcd");
        LoginReq req = new LoginReq();
        req.setUsername("anyone");
        req.setPassword("anyone");
        req.setCaptchaKey("key2");
        req.setCaptchaCode("wrong");
        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class);   // CAPTCHA_INVALID
    }
}
