package com.medistock.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medistock.common.config.TenantContext;
import com.medistock.common.exception.BusinessException;
import com.medistock.system.entity.User;
import com.medistock.system.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {

    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createUser_encryptsPassword_andRejectsDuplicate() {
        TenantContext.setTenantId(1L);
        try {
            User u = new User();
            u.setTenantId(1L);
            u.setUsername("u_" + System.currentTimeMillis());
            // 第五轮起「姓名」为必填（与前端 User.vue 的必填校验一致）
            u.setRealName("测试用户");
            userService.createUser(u, "raw-pass-123");

            // 显式 select 密码列（实体默认 select=false，否则拿不到密文）
            User saved = userService.getOne(new LambdaQueryWrapper<User>()
                    .select(User::getUsername, User::getPassword)
                    .eq(User::getUsername, u.getUsername()));
            assertThat(passwordEncoder.matches("raw-pass-123", saved.getPassword())).isTrue();  // BCrypt 校验通过

            // 重复用户名被拒绝
            User dup = new User();
            dup.setTenantId(1L);
            dup.setUsername(u.getUsername());
            dup.setRealName("重复用户");
            // 密码同样满足 6~64 位下限，确保触发的是「用户名重复」而非密码校验
            assertThatThrownBy(() -> userService.createUser(dup, "dup-pass-123")).isInstanceOf(BusinessException.class);
        } finally {
            TenantContext.clear();
        }
    }
}
