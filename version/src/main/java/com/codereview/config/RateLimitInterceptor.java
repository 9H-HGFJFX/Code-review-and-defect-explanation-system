package com.codereview.config;

import com.codereview.common.enums.ErrorCode;
import com.codereview.security.JwtUserDetails;
import com.codereview.security.JwtUserDetailsAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 限流拦截器
 * 基于Redis滑动窗口计数器实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    /**
     * 限流键前缀
     */
    private static final String RATE_LIMIT_PREFIX = "rate:limit:";

    /**
     * 未登录用户限制（每分钟）
     */
    private static final int ANONYMOUS_LIMIT = 100;

    /**
     * 已登录用户限制（每分钟）
     */
    private static final int AUTHENTICATED_LIMIT = 1000;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientKey = getClientKey(request);
        int limit = getLimit(request);

        // 使用滑动窗口算法
        String key = RATE_LIMIT_PREFIX + clientKey;
        long currentTime = System.currentTimeMillis() / 1000;
        long windowStart = currentTime - 60; // 60秒窗口

        // 移除窗口外的老记录
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        // 获取当前窗口内的请求数
        Long count = redisTemplate.opsForZSet().zCard(key);

        if (count != null && count >= limit) {
            // 超过限制
            log.warn("Rate limit exceeded: clientKey={}, count={}, limit={}", clientKey, count, limit);

            // 设置限流响应头
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("X-RateLimit-Reset", String.valueOf(currentTime + 60));

            sendRateLimitResponse(response);
            return false;
        }

        // 添加当前请求到窗口
        redisTemplate.opsForZSet().add(key, String.valueOf(currentTime), currentTime);
        redisTemplate.expire(key, java.time.Duration.ofMinutes(2));

        // 设置响应头
        long remaining = limit - (count != null ? count + 1 : 1);
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, remaining)));
        response.setHeader("X-RateLimit-Reset", String.valueOf(currentTime + 60));

        return true;
    }

    /**
     * 获取客户端标识
     * 已登录用户使用UserId，未登录使用IP
     */
    private String getClientKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof JwtUserDetailsAdapter jwtUser) {
            // 已登录用户，使用UserId
            return "user:" + jwtUser.getJwtUserDetails().getUserId();
        }

        // 未登录用户，使用IP
        String ip = getClientIp(request);
        return "ip:" + ip;
    }

    /**
     * 获取限流阈值
     */
    private int getLimit(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof JwtUserDetailsAdapter) {
            return AUTHENTICATED_LIMIT;
        }

        // 检查是否为写操作
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
            return 100; // 写操作更严格的限制
        }

        return ANONYMOUS_LIMIT;
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 多级代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * 发送限流响应
     */
    private void sendRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("code", ErrorCode.RATE_LIMIT_EXCEEDED.getCode());
        errorBody.put("message", ErrorCode.RATE_LIMIT_EXCEEDED.getMessage());
        errorBody.put("data", null);
        errorBody.put("timestamp", System.currentTimeMillis());

        response.getWriter().write(new ObjectMapper().writeValueAsString(errorBody));
    }
}
