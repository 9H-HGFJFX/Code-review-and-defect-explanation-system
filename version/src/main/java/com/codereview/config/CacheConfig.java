package com.codereview.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置类
 * 
 * @author code-review-team
 */
@Configuration
public class CacheConfig {

    /**
     * 配置Caffeine本地缓存
     * 用于AST缓存
     */
    @Bean
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)           // 最多缓存1000个
            .expireAfterWrite(5, TimeUnit.MINUTES)  // 写入5分钟后过期
            .recordStats());             // 开启统计
        return cacheManager;
    }
}
