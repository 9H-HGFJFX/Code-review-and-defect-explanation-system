package com.codeaudit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * test profile 下的 NoOp Redis 替代品 —— 业务代码完全无感知。
 *
 * 由于 Spring Data Redis 的 RedisTemplate / RedisConnectionFactory 接口方法多达几十个，
 * 完整 mock 工作量巨大。简化策略：只覆盖业务真正用到的几个方法（hasKey / opsForValue / delete / convertAndSend），
 * 其他方法让基类默认实现抛 UnsupportedOperationException —— 业务代码不会触发这些路径。
 */
@Profile("test")
@Configuration
public class NoOpRedisConfig {

    /**
     * 提供一个 NoOp 版本的 RedisTemplate，覆盖业务用到的几个方法。
     * Spring 自动配置的 LettuceConnectionFactory 仍然存在但本 bean 用 @Primary 优先注入。
     */
    @Bean
    @Primary
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RedisTemplate<String, Object> redisTemplate() {
        return new NoOpRedisTemplate();
    }
}
