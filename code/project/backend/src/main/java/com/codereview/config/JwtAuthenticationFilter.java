package com.codereview.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        // 默认空实现，实际使用时通过构造函数注入
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String token = extractToken(request);
        
        if (token != null) {
            try {
                // 检查Token是否在黑名单
                String blacklistKey = "jwt:blacklist:" + token;
                if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
                    log.debug("Token已在黑名单中");
                    filterChain.doFilter(request, response);
                    return;
                }
                
                // 验证Token
                if (jwtUtil.validateToken(token) && jwtUtil.isAccessToken(token)) {
                    Claims claims = jwtUtil.parseToken(token);
                    
                    String username = claims.getSubject();
                    String role = claims.get("role", String.class);
                    Long userId = claims.get("userId", Long.class);
                    
                    // 创建认证对象
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                    
                    // 将userId存入认证对象
                    authentication.setDetails(userId);
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("用户{}认证成功，角色: {}", username, role);
                }
            } catch (ExpiredJwtException e) {
                log.debug("Token已过期");
            } catch (Exception e) {
                log.warn("Token验证失败: {}", e.getMessage());
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * 从请求头提取Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
