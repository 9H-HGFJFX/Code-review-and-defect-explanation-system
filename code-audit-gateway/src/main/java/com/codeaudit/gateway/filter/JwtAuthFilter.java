package com.codeaudit.gateway.filter;

import com.codeaudit.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 网关全局鉴权过滤器
 * 1) 白名单路径：直接放行
 * 2) 其他路径：解析 JWT -> 校验 -> 注入 X-User-* 头
 * 3) Token 失效/过期/黑名单 -> 401
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements WebFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final ReactiveStringRedisTemplate redisTemplate;

    @Value("${jwt.header}")
    private String header;
    @Value("${jwt.prefix}")
    private String prefix;

    @Value("${auth.public-paths}")
    private List<String> publicPaths;

    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        // CORS 预检
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return chain.filter(exchange);
        }

        // 白名单
        for (String pattern : publicPaths) {
            if (matcher.match(pattern, path)) {
                return chain.filter(exchange);
            }
        }

        // 取 token
        String authHeader = request.getHeaders().getFirst(header);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(prefix)) {
            return unauthorized(exchange, "未提供认证令牌");
        }
        String token = authHeader.substring(prefix.length()).trim();

        // 验签
        Claims claims = jwtUtil.parse(token);
        if (claims == null) {
            return unauthorized(exchange, "认证令牌无效或已过期");
        }
        if (!"access".equals(jwtUtil.getType(claims))) {
            return unauthorized(exchange, "Token 类型错误");
        }
        Long userId = jwtUtil.getUserId(claims);
        String username = jwtUtil.getUsername(claims);
        String role = jwtUtil.getRole(claims);
        if (userId == null || username == null || role == null) {
            return unauthorized(exchange, "Token 内容不完整");
        }

        // 黑名单校验
        return redisTemplate.hasKey("jwt:blacklist:" + token)
                .flatMap(blacklisted -> {
                    if (blacklisted) {
                        return unauthorized(exchange, "Token 已登出");
                    }
                    // 注入用户信息到下游请求头
                    ServerHttpRequest mutated = request.mutate()
                            .header("X-User-Id", String.valueOf(userId))
                            .header("X-User-Name", username)
                            .header("X-User-Role", role)
                            .header("X-Request-Id", java.util.UUID.randomUUID().toString())
                            .header("X-Real-IP", clientIp(request))
                            .build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Redis 不可用时直接放行（不阻塞业务），仅记录警告
                    log.warn("[AUTH] Redis 黑名单检查失败（Redis 不可用），放行请求 userId={}", userId);
                    ServerHttpRequest mutated = request.mutate()
                            .header("X-User-Id", String.valueOf(userId))
                            .header("X-User-Name", username)
                            .header("X-User-Role", role)
                            .header("X-Request-Id", java.util.UUID.randomUUID().toString())
                            .header("X-Real-IP", clientIp(request))
                            .build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                }));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":401,\"message\":\"" + msg + "\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private String clientIp(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) return ip.split(",")[0].trim();
        return request.getRemoteAddress() == null ? "unknown" : request.getRemoteAddress().getAddress().getHostAddress();
    }

    @Override public int getOrder() { return 0; }
}
