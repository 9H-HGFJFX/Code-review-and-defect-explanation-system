package com.codeaudit.config;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 最小化 NoOp RedisTemplate —— 覆盖业务代码真正用到的几个方法：
 *   - hasKey
 *   - delete(String)
 *   - convertAndSend
 *   - opsForValue().get / set
 *
 * 其他 opsForXxx() 业务不调用，基类会抛 UnsupportedOperationException（但不会被触发）。
 */
public class NoOpRedisTemplate extends RedisTemplate<String, Object> {
    public NoOpRedisTemplate() {
        // 不调用 afterPropertiesSet() —— 基类会强制校验 ConnectionFactory，
        // NoOp 模式下我们故意不连接真实 Redis
    }

    @Override
    public void afterPropertiesSet() {
        // 跳过基类的强制校验
    }

    @Override
    @SuppressWarnings("unchecked")
    public ValueOperations<String, Object> opsForValue() {
        return (ValueOperations<String, Object>) (ValueOperations) NoOpValueOps.INSTANCE;
    }

    @Override
    public Boolean hasKey(String key) { return false; }

    @Override
    public Long convertAndSend(String channel, Object message) { return 0L; }

    /** 单例 ValueOperations 模拟 */
    public enum NoOpValueOps implements ValueOperations<String, Object> {
        INSTANCE;

        @Override public void set(String key, Object value) {}
        @Override public void set(String key, Object value, long timeout, TimeUnit unit) {}
        @Override public Boolean setIfAbsent(String key, Object value) { return true; }
        @Override public Boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) { return true; }
        @Override public Boolean setIfPresent(String key, Object value) { return true; }
        @Override public Boolean setIfPresent(String key, Object value, long timeout, TimeUnit unit) { return true; }
        @Override public void multiSet(Map<? extends String, ?> map) {}
        @Override public Boolean multiSetIfAbsent(Map<? extends String, ?> map) { return true; }
        @Override public Object get(Object key) { return null; }
        @Override public Object getAndDelete(String key) { return null; }
        @Override public Object getAndExpire(String key, long timeout, TimeUnit unit) { return null; }
        @Override public Object getAndExpire(String key, java.time.Duration timeout) { return null; }
        @Override public Object getAndPersist(String key) { return null; }
        @Override public Object getAndSet(String key, Object value) { return null; }
        @Override public List<Object> multiGet(Collection<String> keys) { return java.util.Collections.emptyList(); }
        @Override public Long increment(String key) { return 0L; }
        @Override public Long increment(String key, long delta) { return 0L; }
        @Override public Double increment(String key, double delta) { return 0.0; }
        @Override public Long decrement(String key) { return 0L; }
        @Override public Long decrement(String key, long delta) { return 0L; }
        @Override public Integer append(String key, String value) { return 0; }
        @Override public String get(String key, long start, long end) { return ""; }
        @Override public void set(String key, Object value, long offset) {}
        @Override public Long size(String key) { return 0L; }
        @Override public Boolean setBit(String key, long offset, boolean value) { return true; }
        @Override public Boolean getBit(String key, long offset) { return false; }
        @Override public List<Long> bitField(String key, org.springframework.data.redis.connection.BitFieldSubCommands subCommands) { return java.util.Collections.emptyList(); }
        @Override public org.springframework.data.redis.core.RedisOperations<String, Object> getOperations() { return null; }
    }
}
