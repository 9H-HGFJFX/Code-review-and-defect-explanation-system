package com.codereview.security;

import com.codereview.config.JwtTokenProvider;
import com.codereview.config.TokenBlacklistService;
import com.codereview.common.enums.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT认证过滤器
 * 继承OncePerRequestFilter，确保每个请求只执行一次
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 路径白名单，这些路径不需要认证
     */
    private static final String[] WHITE_LIST = {
            "/api/auth/login",
            "/api/auth/refresh",
            "/actuator/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/error"
    };

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 检查是否在白名单中
        String requestUri = request.getRequestURI();
        if (isWhiteListed(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 从请求头获取Token
            String token = extractTokenFromRequest(request);

            if (token == null) {
                sendErrorResponse(response, ErrorCode.TOKEN_MISSING);
                return;
            }

            // 验证Token有效性
            if (!jwtTokenProvider.validateToken(token)) {
                if (jwtTokenProvider.isTokenExpired(token)) {
                    sendErrorResponse(response, ErrorCode.TOKEN_EXPIRED);
                } else {
                    sendErrorResponse(response, ErrorCode.TOKEN_INVALID);
                }
                return;
            }

            // 检查是否为Refresh Token（Refresh Token不能用于API访问）
            String tokenType = extractTokenType(token);
            if ("refresh".equals(tokenType)) {
                sendErrorResponse(response, ErrorCode.TOKEN_INVALID);
                return;
            }

            // 如果是Refresh Token，验证黑名单（Refresh Token不应出现在这里，但做防御性检查）
            String jti = jwtTokenProvider.extractJti(token);
            if (jti != null && tokenBlacklistService.isBlacklisted(jti)) {
                sendErrorResponse(response, ErrorCode.TOKEN_INVALID);
                return;
            }

            // 解析用户信息并设置到SecurityContext
            JwtUserDetails jwtUserDetails = jwtTokenProvider.getJwtUserDetails(token);
            JwtUserDetailsAdapter userDetails = new JwtUserDetailsAdapter(jwtUserDetails);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 将JwtUserDetails存储到request属性中，方便后续使用
            request.setAttribute("jwtUserDetails", jwtUserDetails);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("JWT authentication failed", e);
            sendErrorResponse(response, ErrorCode.UNAUTHORIZED);
        }
    }

    /**
     * 从请求中提取Token
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 从Token中提取类型
     * 使用JWT payload中的type字段区分Access/Refresh token
     */
    private String extractTokenType(String token) {
        try {
            // 通过检查是否存在jti字段来判断是否为Refresh Token
            String jti = jwtTokenProvider.extractJti(token);
            if (jti != null && !jti.isEmpty()) {
                return "refresh";
            }
            return "access";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhiteListed(String requestUri) {
        for (String pattern : WHITE_LIST) {
            if (pathMatcher.match(pattern, requestUri)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("code", errorCode.getCode());
        errorBody.put("message", errorCode.getMessage());
        errorBody.put("data", null);
        errorBody.put("timestamp", System.currentTimeMillis());

        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }
}
