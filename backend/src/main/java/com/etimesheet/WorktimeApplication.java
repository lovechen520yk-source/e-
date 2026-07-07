package com.etimesheet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 工时管理系统 - Spring Boot 启动类
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class WorktimeApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorktimeApplication.class, args);
    }
}
