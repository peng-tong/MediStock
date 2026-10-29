package com.medistock.system;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medistock.common.config.TenantContext;
import com.medistock.system.entity.Tenant;
import com.medistock.system.service.TenantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TenantServiceTest {

    @Autowired
    private TenantService tenantService;

    @Test
    void createAndPageQuery() {
        TenantContext.setTenantId(TenantContext.SUPER_ADMIN_TENANT_ID);
        try {
            Tenant t = new Tenant();
            t.setName("测试租户");
            t.setCode("test-" + System.currentTimeMillis());
            t.setStatus(1);
            t.setMaxUserCount(10);
            tenantService.save(t);

            Page<Tenant> page = tenantService.pageQuery(1, 10, "测试租户");
            assertThat(page.getTotal()).isGreaterThanOrEqualTo(1);
            assertThat(page.getRecords().get(0).getMaxUserCount()).isEqualTo(10);
        } finally {
            TenantContext.clear();
        }
    }
}
