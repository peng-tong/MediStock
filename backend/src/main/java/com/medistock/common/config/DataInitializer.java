package com.medistock.common.config;

import com.medistock.base.entity.DrugCategory;
import com.medistock.base.mapper.DrugCategoryMapper;
import com.medistock.system.entity.Menu;
import com.medistock.system.entity.Role;
import com.medistock.system.entity.Tenant;
import com.medistock.system.entity.User;
import com.medistock.system.mapper.MenuMapper;
import com.medistock.system.mapper.RoleMapper;
import com.medistock.system.mapper.TenantMapper;
import com.medistock.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 种子数据：系统首次启动时执行。
 * - 平台租户(超管，tenant_id=0 的标识由用户表承载)
 * - 示例租户 + 租户管理员
 * - 菜单目录（前端动态渲染的依据）
 * - 药品分类（处方药/OTC，挂到示例租户下供租户管理员使用）
 * 注意：租户拦截器对 tenantId=0 放行，故种子数据统一在 TenantContext=0 下写入，
 * 且业务表（如 base_drug_category）的 tenant_id 必须显式赋值（拦截器不会自动注入）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final TenantMapper tenantMapper;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;
    private final DrugCategoryMapper categoryMapper;
    private final PasswordEncoder passwordEncoder;

    /** 示例租户 id（createTenantAndUsers 生成，业务种子归属） */
    private Long demoTenantId;

    @Override
    public void run(String... args) {
        TenantContext.setTenantId(TenantContext.SUPER_ADMIN_TENANT_ID);
        try {
            if (tenantMapper.selectCount(null) > 0) {
                return; // 已初始化
            }
            createTenantAndUsers();
            createMenus();
            createCategories();
            log.info("种子数据初始化完成");
        } finally {
            TenantContext.clear();
        }
    }

    private void createTenantAndUsers() {
        // 示例租户
        Tenant tenant = new Tenant();
        tenant.setName("示例药房");
        tenant.setCode("demo-pharmacy");
        tenant.setStatus(1);
        tenant.setMaxUserCount(50);
        tenantMapper.insert(tenant);
        demoTenantId = tenant.getId();

        // 平台超管（tenantId=0，登录后跨租户）
        User superAdmin = new User();
        superAdmin.setTenantId(TenantContext.SUPER_ADMIN_TENANT_ID);
        superAdmin.setUsername("superadmin");
        superAdmin.setPassword(passwordEncoder.encode("admin123"));
        superAdmin.setRealName("平台超管");
        superAdmin.setStatus(1);
        userMapper.insert(superAdmin);

        Role superRole = new Role();
        superRole.setTenantId(TenantContext.SUPER_ADMIN_TENANT_ID);
        superRole.setName("超级管理员");
        superRole.setCode("SUPER_ADMIN");
        superRole.setStatus(1);
        roleMapper.insert(superRole);

        // 租户管理员（登录后只见本租户数据）
        User tenantAdmin = new User();
        tenantAdmin.setTenantId(demoTenantId);
        tenantAdmin.setUsername("admin");
        tenantAdmin.setPassword(passwordEncoder.encode("admin123"));
        tenantAdmin.setRealName("租户管理员");
        tenantAdmin.setStatus(1);
        userMapper.insert(tenantAdmin);

        Role tenantRole = new Role();
        tenantRole.setTenantId(demoTenantId);
        tenantRole.setName("租户管理员");
        tenantRole.setCode("TENANT_ADMIN");
        tenantRole.setStatus(1);
        roleMapper.insert(tenantRole);
    }

    private void createMenus() {
        // 最小菜单集合：目录(1) + 菜单(2)。按钮级(3)为 P1 预留，本期不插。
        // 先建 5 个目录并记录 id，再把子菜单挂到对应目录下
        long baseDir = insertDir("基础资料", "/base", 1);
        long purchaseDir = insertDir("采购管理", "/purchase", 2);
        long salesDir = insertDir("销售管理", "/sales", 3);
        long inventoryDir = insertDir("库存管理", "/inventory", 4);
        long systemDir = insertDir("系统管理", "/system", 5);

        insertMenu("药品档案", "/base/drug", "base/drug/index", 2, baseDir, 1);
        insertMenu("采购订单", "/purchase/order", "purchase/order/index", 2, purchaseDir, 1);
        insertMenu("销售开单", "/sales/order", "sales/order/index", 2, salesDir, 1);
        insertMenu("库存查询", "/inventory/stock", "inventory/stock/index", 2, inventoryDir, 1);
        insertMenu("库存盘点", "/inventory/stocktake", "inventory/stocktake/index", 2, inventoryDir, 2);
        insertMenu("用户管理", "/system/user", "system/user/index", 2, systemDir, 1);
        insertMenu("角色管理", "/system/role", "system/role/index", 2, systemDir, 2);
        insertMenu("租户管理", "/system/tenant", "system/tenant/index", 2, systemDir, 3);
    }

    /** 插入目录节点（parentId=0），返回其 id */
    private long insertDir(String name, String path, int sort) {
        Menu menu = new Menu();
        menu.setName(name);
        menu.setPath(path);
        menu.setType(1);
        menu.setSort(sort);
        menu.setStatus(1);
        menu.setParentId(0L);
        menuMapper.insert(menu);
        return menu.getId();
    }

    /** 插入菜单节点（挂在指定父目录下） */
    private void insertMenu(String name, String path, String component, int type, long parentId, int sort) {
        Menu menu = new Menu();
        menu.setName(name);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setType(type);
        menu.setSort(sort);
        menu.setStatus(1);
        menu.setParentId(parentId);
        menuMapper.insert(menu);
    }

    private void createCategories() {
        insertCategory("处方药");
        insertCategory("OTC");
    }

    private void insertCategory(String name) {
        DrugCategory c = new DrugCategory();
        c.setTenantId(demoTenantId);   // 业务种子归属示例租户（超管上下文拦截器放行，不会自动注入 tenant_id）
        c.setName(name);
        c.setParentId(0L);
        c.setStatus(1);
        categoryMapper.insert(c);
    }
}
