package com.medistock.common.config;

/** 当前请求的租户上下文：JWT 过滤器写入，租户拦截器读取 */
public class TenantContext {
    /** 超管租户标识：0 表示平台级，跳过租户过滤 */
    public static final long SUPER_ADMIN_TENANT_ID = 0L;
    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    public static void setTenantId(Long tenantId) {
        CURRENT.set(tenantId);
    }

    public static Long getTenantId() {
        return CURRENT.get();
    }

    public static boolean isSuperAdmin() {
        Long tenantId = CURRENT.get();
        return tenantId == null || tenantId == SUPER_ADMIN_TENANT_ID;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
