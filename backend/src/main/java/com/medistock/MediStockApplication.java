package com.medistock;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 药品进销存系统启动类
 */
@SpringBootApplication
// 扫描各模块的 MyBatis Mapper 接口，自动注册为代理
@MapperScan("com.medistock.**.mapper")
public class MediStockApplication {
    public static void main(String[] args) {
        SpringApplication.run(MediStockApplication.class, args);
    }
}
