package com.medistock.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medistock.common.config.TenantContext;
import com.medistock.system.entity.Role;
import com.medistock.system.entity.RoleMenu;
import com.medistock.system.mapper.RoleMenuMapper;
import com.medistock.system.service.RoleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RoleServiceTest {

    @Autowired
    private RoleService roleService;
    @Autowired
    private RoleMenuMapper roleMenuMapper;

    @Test
    void grantMenus_replacesOldBindings() {
        TenantContext.setTenantId(1L);
        try {
            Role role = new Role();
            role.setTenantId(1L);
            role.setName("测试角色");
            role.setCode("TEST");
            role.setStatus(1);
            roleService.save(role);

            roleService.grantMenus(role.getId(), List.of(1L, 2L));
            assertThat(countByRole(role.getId())).isEqualTo(2);

            roleService.grantMenus(role.getId(), List.of(3L));
            assertThat(countByRole(role.getId())).isEqualTo(1);  // 先删后插生效
        } finally {
            TenantContext.clear();
        }
    }

    private long countByRole(Long roleId) {
        return roleMenuMapper.selectCount(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId));
    }
}
