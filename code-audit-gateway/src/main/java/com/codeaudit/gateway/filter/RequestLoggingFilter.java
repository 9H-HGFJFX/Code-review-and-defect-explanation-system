package com.codeaudit.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 请求日志过滤器 - 记录所有经过网关的请求
 */
@Slf4j
@Component
public class RequestLoggingFilter implements WebFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long start = System.currentTimeMillis();
        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        String ip = clientIp(request);
        String userId = request.getHeaders().getFirst("X-User-Id");

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long cost = System.currentTimeMillis() - start;
            ServerHttpResponse response = exchange.getResponse();
            int status = response.getStatusCode() == null ? 0 : response.getStatusCode().value();
            log.info("[GW] {} {} {} uid={} ip={} status={} cost={}ms",
                    method, path, status, userId, ip, status, cost);
        }));
    }

    private String clientIp(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) return ip.split(",")[0].trim();
        return request.getRemoteAddress() == null ? "unknown" : request.getRemoteAddress().getAddress().getHostAddress();
    }

    @Override public int getOrder() { return -100; }
}
