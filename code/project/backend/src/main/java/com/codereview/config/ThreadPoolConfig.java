package com.codereview.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置类
 * 用于代码审查引擎的异步任务处理
 */
@Configuration
public class ThreadPoolConfig {
    
    @Value("${review.thread-pool.core-pool-size:5}")
    private Integer corePoolSize;
    
    @Value("${review.thread-pool.max-pool-size:20}")
    private Integer maxPoolSize;
    
    @Value("${review.thread-pool.queue-capacity:100}")
    private Integer queueCapacity;
    
    /**
     * 代码审查专用线程池
     * 核心线程数：5
     * 最大线程数：20
     * 队列容量：100
     * 拒绝策略：调用者直接运行
     */
    @Bean("reviewExecutor")
    public Executor reviewExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("ReviewEngine-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
