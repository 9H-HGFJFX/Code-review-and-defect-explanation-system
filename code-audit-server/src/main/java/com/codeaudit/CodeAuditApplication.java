package com.codeaudit;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 代码审查与缺陷解释系统 - 服务启动类
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class
})
@MapperScan("com.codeaudit.repository")
public class CodeAuditApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeAuditApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("  代码审查与缺陷解释系统 启动成功");
        System.out.println("  API 文档: http://localhost:8080/doc.html");
        System.out.println("========================================\n");
    }
}
