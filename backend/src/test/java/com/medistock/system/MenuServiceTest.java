package com.medistock.system;

import com.medistock.common.config.TenantContext;
import com.medistock.system.entity.Menu;
import com.medistock.system.service.MenuService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MenuServiceTest {

    @Autowired
    private MenuService menuService;

    @Test
    void tree_buildsHierarchy() {
        TenantContext.setTenantId(TenantContext.SUPER_ADMIN_TENANT_ID);
        try {
            // 建一个目录 + 两个子菜单
            Menu dir = new Menu();
            dir.setName("测试目录");
            dir.setType(1);
            dir.setParentId(0L);
            dir.setSort(99);
            dir.setStatus(1);
            menuService.save(dir);

            for (String n : List.of("子菜单A", "子菜单B")) {
                Menu child = new Menu();
                child.setName(n);
                child.setType(2);
                child.setParentId(dir.getId());
                child.setSort(1);
                child.setStatus(1);
                menuService.save(child);
            }

            List<Menu> tree = menuService.tree();
            Menu found = tree.stream().filter(m -> m.getId().equals(dir.getId())).findFirst().orElseThrow();
            assertThat(found.getChildren()).hasSize(2);
        } finally {
            TenantContext.clear();
        }
    }
}
