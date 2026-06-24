package com.codereview.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token黑名单服务
 * 使用Redis存储已登出的Refresh Token的jti
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 黑名单键前缀
     */
    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    /**
     * 将Refresh Token的jti加入黑名单
     *
     * @param jti         Refresh Token的jti
     * @param ttlSeconds  剩余过期时间（秒）
     */
    public void addToBlacklist(String jti, long ttlSeconds) {
        if (jti == null || jti.isEmpty()) {
            return;
        }

        String key = BLACKLIST_PREFIX + jti;
        redisTemplate.opsForValue().set(key, "1", ttlSeconds, TimeUnit.SECONDS);
        log.debug("Added token to blacklist: jti={}, ttl={}s", jti, ttlSeconds);
    }

    /**
     * 检查jti是否在黑名单中
     *
     * @param jti Refresh Token的jti
     * @return 是否在黑名单中
     */
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isEmpty()) {
            return false;
        }

        String key = BLACKLIST_PREFIX + jti;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * 从黑名单中移除jti（通常不需要，除非特殊情况）
     *
     * @param jti Refresh Token的jti
     */
    public void removeFromBlacklist(String jti) {
        if (jti == null || jti.isEmpty()) {
            return;
        }

        String key = BLACKLIST_PREFIX + jti;
        redisTemplate.delete(key);
        log.debug("Removed token from blacklist: jti={}", jti);
    }
}
