package com.codeaudit.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 角色检查过滤器 - 严格路径角色校验
 * - teacher-admin-paths: TEACHER / ADMIN 可访问
 * - admin-only-paths: 仅 ADMIN 可访问
 */
@Slf4j
@Component
public class RoleCheckFilter implements WebFilter, Ordered {

    @Value("${auth.teacher-admin-paths}")
    private List<String> teacherAdminPaths;

    @Value("${auth.admin-only-paths}")
    private List<String> adminOnlyPaths;

    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String role = request.getHeaders().getFirst("X-User-Role");

        // 教师/管理员路径
        for (String pattern : teacherAdminPaths) {
            if (matcher.match(pattern, path)) {
                if (!"TEACHER".equals(role) && !"ADMIN".equals(role)) {
                    return forbidden(exchange, "权限不足：需要教师或管理员");
                }
            }
        }
        // 仅管理员路径
        for (String pattern : adminOnlyPaths) {
            if (matcher.match(pattern, path)) {
                if (!"ADMIN".equals(role)) {
                    return forbidden(exchange, "权限不足：需要管理员");
                }
            }
        }
        return chain.filter(exchange);
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":403,\"message\":\"" + msg + "\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override public int getOrder() { return 10; }
}
