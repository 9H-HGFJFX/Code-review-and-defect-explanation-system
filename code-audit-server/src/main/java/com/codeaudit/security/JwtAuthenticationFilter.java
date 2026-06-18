package com.codeaudit.security;

import com.codeaudit.config.JwtConfig;
import io.jsonwebtoken.Claims;
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
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtConfig jwtConfig;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token) && !jwtUtil.isBlacklisted(token)) {
            Claims claims = jwtUtil.parse(token);
            if (claims != null && "access".equals(jwtUtil.getType(claims))) {
                Long userId = jwtUtil.getUserId(claims);
                String username = jwtUtil.getUsername(claims);
                String role = jwtUtil.getRole(claims);
                if (userId != null && username != null && role != null) {
                    LoginUser loginUser = new LoginUser(userId, username, "", role, 1);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            loginUser, null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(jwtConfig.getHeader());
        if (StringUtils.hasText(bearer) && bearer.startsWith(jwtConfig.getPrefix())) {
            return bearer.substring(jwtConfig.getPrefix().length());
        }
        return null;
    }
}
