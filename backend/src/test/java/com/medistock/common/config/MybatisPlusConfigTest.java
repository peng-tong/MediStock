package com.medistock.common.config;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MybatisPlusConfigTest {

    /** 临时实体：模拟"带 tenant_id 的业务表"，验证租户拦截器真实拼 SQL */
    @Data
    @TableName("t_demo")
    public static class Demo {
        @TableId(type = IdType.AUTO)
        private Long id;
        private Long tenantId;
        private String name;
        @TableLogic
        private Integer deleted;
        private LocalDateTime createTime;
    }

    @Autowired
    private DemoMapper demoMapper;

    /**
     * 显式注册 DemoMapper 为 MapperFactoryBean：
     * 主应用 @MapperScan("com.medistock.**.mapper") 只扫描各模块 mapper 子包，不会命中本测试包；
     * MP 基于 @Mapper 注解的自动扫描（basePackage=com.medistock）在本工程存在显式 @MapperScan 时未生效，
     * 因此这里通过 @TestConfiguration 直接注册，保证测试真实走 MyBatis-Plus 完整链路。
     */
    @TestConfiguration
    static class DemoMapperConfig {
        @Bean
        public MapperFactoryBean<DemoMapper> demoMapper(SqlSessionFactory sqlSessionFactory) {
            MapperFactoryBean<DemoMapper> factoryBean = new MapperFactoryBean<>(DemoMapper.class);
            factoryBean.setSqlSessionFactory(sqlSessionFactory);
            return factoryBean;
        }
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void tenantInterceptor_filtersByTenant() {
        // 租户 1 与租户 2 各插一条
        TenantContext.setTenantId(1L);
        Demo d1 = new Demo();
        d1.setName("t1");
        demoMapper.insert(d1);

        TenantContext.setTenantId(2L);
        Demo d2 = new Demo();
        d2.setName("t2");
        demoMapper.insert(d2);

        // 租户 1 只能查到自己的数据
        TenantContext.setTenantId(1L);
        Long count1 = demoMapper.selectCount(new LambdaQueryWrapper<Demo>().eq(Demo::getName, "t1"));
        assertThat(count1).isEqualTo(1);
        Long count2 = demoMapper.selectCount(new LambdaQueryWrapper<Demo>().eq(Demo::getName, "t2"));
        assertThat(count2).isZero();  // 隔离生效

        // 逻辑删除
        demoMapper.deleteById(d1.getId());
        assertThat(demoMapper.selectById(d1.getId())).isNull();

        // 清理测试数据：d2 属于租户 2，需切换到租户 2 上下文才能删掉（租户拦截器会追加 tenant_id 条件）
        TenantContext.setTenantId(2L);
        demoMapper.deleteById(d2.getId());
    }
}

/**
 * 测试用 Mapper（t_demo 表由 schema-h2.sql 创建）。
 * 注：必须定义为顶层接口——mybatis-spring 的 mapper 扫描器（ClassPathMapperScanner.isCandidateComponent）
 * 要求候选类是独立类型（isIndependent），嵌套接口无法被扫描注册为 bean。
 * @Mapper 注解保留：既兼容自动扫描场景，也作为显式注册时的语义标注。
 */
@Mapper
interface DemoMapper extends BaseMapper<MybatisPlusConfigTest.Demo> {}
