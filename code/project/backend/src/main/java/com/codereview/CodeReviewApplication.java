package com.codereview;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 代码审查与缺陷解释系统 - 主启动类
 * 
 * @author Code Review System
 * @version 1.0.0
 */
@SpringBootApplication
@MapperScan("com.codereview.mapper")
@EnableAsync
@EnableScheduling
public class CodeReviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeReviewApplication.class, args);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║         代码审查与缺陷解释系统 启动成功                  ║");
        System.out.println("║  API文档地址: http://localhost:8080/doc.html              ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }
}
