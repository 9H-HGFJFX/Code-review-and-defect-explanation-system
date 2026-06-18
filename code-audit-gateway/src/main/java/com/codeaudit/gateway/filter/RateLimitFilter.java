package com.codeaudit.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * 限流过滤器 - Redis + Lua 令牌桶
 * Key: ratelimit:{userId}:{path}:{minute}
 */
@Slf4j
@Component
public class RateLimitFilter implements WebFilter, Ordered {

    private final ReactiveStringRedisTemplate redis;

    @Value("${ratelimit.review-per-minute}")
    private int reviewPerMinute;

    @Value("${ratelimit.common-per-minute}")
    private int commonPerMinute;

    @Value("${ratelimit.public-per-minute}")
    private int publicPerMinute;

    /** 限流 Lua 脚本（原子 INCR + EXPIRE） */
    private static final String LUA = """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            if current > tonumber(ARGV[2]) then
                return 0
            end
            return 1
            """;

    private final RedisScript<Long> script;

    public RateLimitFilter(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
        this.script = new DefaultRedisScript<>(LUA, Long.class);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String userId = request.getHeaders().getFirst("X-User-Id");
        if (userId == null) {
            // 未鉴权请求直接放行（鉴权失败由 JwtAuthFilter 兜底）
            return chain.filter(exchange);
        }

        int limit = resolveLimit(path);
        long minute = Instant.now().getEpochSecond() / 60;
        // 去掉 /api 前缀
        String simplePath = path.replaceFirst("^/api", "");
        String key = "ratelimit:" + userId + ":" + simplePath + ":" + minute;

        return redis.execute(script, List.of(key), "60", String.valueOf(limit))
                .next()
                .flatMap(allowed -> {
                    if (allowed != null && allowed == 1L) {
                        return chain.filter(exchange);
                    }
                    return tooMany(exchange, limit);
                })
                .onErrorResume(e -> {
                    // Redis 不可用时放行（业务优先）
                    log.warn("[RATELIMIT] Redis 异常，放行请求: {}", e.getMessage());
                    return chain.filter(exchange);
                });
    }

    private int resolveLimit(String path) {
        if (path.contains("/review/submit")) return reviewPerMinute;
        if (path.startsWith("/api/auth/")) return publicPerMinute;
        return commonPerMinute;
    }

    private Mono<Void> tooMany(ServerWebExchange exchange, int limit) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().add("Retry-After", "60");
        String body = "{\"code\":429,\"message\":\"请求过于频繁，每分钟最多 " + limit + " 次\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override public int getOrder() { return 20; }
}
