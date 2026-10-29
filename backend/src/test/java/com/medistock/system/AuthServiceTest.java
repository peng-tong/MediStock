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

            // createUser 非超管时强制注入当前租户，需切换到新租户上下文创建
            TenantContext.setTenantId(tenant.getId());
            User u = new User();
            u.setUsername("auth_" + System.currentTimeMillis());
            userService.createUser(u, "pass123456");

            // 预置验证码（login 为公开接口，模拟真实 HTTP 场景清空上下文）
            TenantContext.clear();
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

    /**
     * B1 回归：两个租户存在同名用户时，login（公开接口、无租户上下文）不应 500，
     * 而是全库匹配密码，各自密码都应能登录成功。
     */
    @Test
    void login_sameUsernameDifferentTenants_ok() {
        // 建两个启用状态的租户（sys_tenant 为平台表，任意上下文均可插入）
        Tenant t1 = new Tenant();
        t1.setName("同名租户A");
        t1.setCode("ta" + System.currentTimeMillis());
        t1.setStatus(1);
        t1.setMaxUserCount(10);
        tenantMapper.insert(t1);

        Tenant t2 = new Tenant();
        t2.setName("同名租户B");
        t2.setCode("tb" + System.currentTimeMillis());
        t2.setStatus(1);
        t2.setMaxUserCount(10);
        tenantMapper.insert(t2);

        String dupUsername = "dup_" + System.currentTimeMillis();
        try {
            // 分别在两个租户上下文创建同名用户（createUser 非超管时强制注入当前租户）
            TenantContext.setTenantId(t1.getId());
            User u1 = new User();
            u1.setUsername(dupUsername);
            userService.createUser(u1, "pass-a");

            TenantContext.setTenantId(t2.getId());
            User u2 = new User();
            u2.setUsername(dupUsername);
            userService.createUser(u2, "pass-b");

            // 用租户 A 用户的密码登录。login 是公开接口（无租户上下文），
            // 测试需清空上下文模拟真实 HTTP 场景，否则残留上下文会被租户拦截器过滤
            TenantContext.clear();
            captchaStore.put("key3", "wxyz");
            LoginReq req1 = new LoginReq();
            req1.setUsername(dupUsername);
            req1.setPassword("pass-a");
            req1.setCaptchaKey("key3");
            req1.setCaptchaCode("wxyz");
            Map<String, Object> result1 = authService.login(req1);
            assertThat(result1.get("token")).isNotNull();

            // 用租户 B 用户的密码登录同样成功
            captchaStore.put("key4", "mnop");
            LoginReq req2 = new LoginReq();
            req2.setUsername(dupUsername);
            req2.setPassword("pass-b");
            req2.setCaptchaKey("key4");
            req2.setCaptchaCode("mnop");
            Map<String, Object> result2 = authService.login(req2);
            assertThat(result2.get("token")).isNotNull();
        } finally {
            TenantContext.clear();
        }
    }
}
