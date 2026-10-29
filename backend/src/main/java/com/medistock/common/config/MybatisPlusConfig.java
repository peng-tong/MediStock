package com.medistock.common.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.medistock.common.constant.BizConstant;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus 三大利器：
 * 1. 多租户隔离：SQL 自动拼接 tenant_id 条件（超管 tenantId=0 时放行）
 * 2. 分页：方言自动检测（生产 SQLite / 测试 H2）
 * 3. 乐观锁：@Version 字段的 UPDATE 自动带 version 条件（CAS 思想）
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                // 空上下文按 0 处理（如初始化数据），超管场景忽略表逻辑见 ignoreTable
                return new LongValue(TenantContext.getTenantId() == null ? 0L : TenantContext.getTenantId());
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                // 超管（tenantId=0）放行所有表；平台级表始终忽略
                if (TenantContext.isSuperAdmin()) {
                    return true;
                }
                return BizConstant.IGNORE_TABLE_TENANT.equals(tableName)
                        || BizConstant.IGNORE_TABLE_MENU.equals(tableName);
            }
        }));

        // 分页插件：不写死方言，按 JDBC URL 自动检测
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
