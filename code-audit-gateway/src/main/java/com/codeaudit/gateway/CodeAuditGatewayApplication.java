package com.codeaudit.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 代码审查与缺陷解释系统 - API 网关
 * Spring Cloud Gateway（WebFlux 响应式）
 */
@SpringBootApplication
public class CodeAuditGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeAuditGatewayApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("  CodeAudit Gateway 启动成功");
        System.out.println("  HTTP 端口: 8080");
        System.out.println("========================================\n");
    }
}
