package com.codereview.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池配置类
 * 
 * @author code-review-team
 */
@Configuration
public class ExecutorConfig {

    /**
     * 扫描任务线程池
     * 用于执行代码扫描任务
     */
    @Bean(name = "scanTaskExecutor")
    public ThreadPoolExecutor scanTaskExecutor() {
        return new ThreadPoolExecutor(
            5,                          // 核心线程数
            10,                         // 最大线程数
            60L,                        // 空闲线程存活时间
            TimeUnit.SECONDS,
            new java.util.concurrent.LinkedBlockingQueue<>(100),
            r -> {
                Thread t = new Thread(r);
                t.setName("scan-task-" + t.getId());
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * API任务线程池
     * 用于处理HTTP接口请求
     */
    @Bean(name = "apiTaskExecutor")
    public ThreadPoolExecutor apiTaskExecutor() {
        return new ThreadPoolExecutor(
            10,                         // 核心线程数
            20,                         // 最大线程数
            30L,                        // 空闲线程存活时间
            TimeUnit.SECONDS,
            new java.util.concurrent.LinkedBlockingQueue<>(500),
            r -> {
                Thread t = new Thread(r);
                t.setName("api-task-" + t.getId());
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 规则编译线程池
     * 用于编译和验证代码规则
     */
    @Bean(name = "ruleCompileExecutor")
    public ThreadPoolExecutor ruleCompileExecutor() {
        return new ThreadPoolExecutor(
            2,                          // 核心线程数
            4,                          // 最大线程数
            120L,                       // 空闲线程存活时间
            TimeUnit.SECONDS,
            new java.util.concurrent.LinkedBlockingQueue<>(50),
            r -> {
                Thread t = new Thread(r);
                t.setName("rule-compile-" + t.getId());
                return t;
            },
            new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
