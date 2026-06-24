package com.codereview.config;

import com.codereview.common.enums.UserRole;
import com.codereview.security.JwtUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT令牌提供者
 * 负责生成、验证和解析JWT令牌
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成Access Token
     * payload包含: userId, username, role, classId, exp
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param role     角色
     * @param classId  班级ID
     * @return Access Token字符串
     */
    public String generateAccessToken(Long userId, String username, UserRole role, Long classId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration * 1000);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role.name())
                .claim("classId", classId)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成Refresh Token
     * payload包含: userId, type=jti, jti, exp
     *
     * @param userId 用户ID
     * @return Refresh Token字符串
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration * 1000);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", "refresh")
                .claim("jti", jti)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从Refresh Token中提取jti
     *
     * @param refreshToken Refresh Token
     * @return jti值
     */
    public String extractJti(String refreshToken) {
        try {
            Claims claims = parseToken(refreshToken);
            return claims.get("jti", String.class);
        } catch (Exception e) {
            log.error("Failed to extract jti from refresh token", e);
            return null;
        }
    }

    /**
     * 获取Refresh Token的剩余过期时间（秒）
     *
     * @param refreshToken Refresh Token
     * @return 剩余过期时间
     */
    public long getRefreshTokenTtl(String refreshToken) {
        try {
            Claims claims = parseToken(refreshToken);
            Date expiration = claims.getExpiration();
            long remaining = (expiration.getTime() - System.currentTimeMillis()) / 1000;
            return Math.max(0, remaining);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 验证Token有效性
     *
     * @param token Token字符串
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("Token expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.debug("Unsupported token: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.debug("Malformed token: {}", e.getMessage());
        } catch (SecurityException e) {
            log.debug("Invalid token signature: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.debug("Token claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 检查Token是否过期
     *
     * @param token Token字符串
     * @return 是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从Token中获取用户ID
     *
     * @param token Token字符串
     * @return 用户ID
     */
    public Long getUserId(String token) {
        try {
            Claims claims = parseToken(token);
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            log.error("Failed to get userId from token", e);
            return null;
        }
    }

    /**
     * 从Token中获取用户名
     *
     * @param token Token字符串
     * @return 用户名
     */
    public String getUsername(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("username", String.class);
        } catch (Exception e) {
            log.error("Failed to get username from token", e);
            return null;
        }
    }

    /**
     * 从Token中获取角色
     *
     * @param token Token字符串
     * @return 角色枚举
     */
    public UserRole getRole(String token) {
        try {
            Claims claims = parseToken(token);
            String roleName = claims.get("role", String.class);
            return UserRole.fromName(roleName);
        } catch (Exception e) {
            log.error("Failed to get role from token", e);
            return null;
        }
    }

    /**
     * 从Token中获取班级ID
     *
     * @param token Token字符串
     * @return 班级ID
     */
    public Long getClassId(String token) {
        try {
            Claims claims = parseToken(token);
            Integer classId = claims.get("classId", Integer.class);
            return classId != null ? classId.longValue() : null;
        } catch (Exception e) {
            log.error("Failed to get classId from token", e);
            return null;
        }
    }

    /**
     * 从Token中获取JwtUserDetails
     *
     * @param token Token字符串
     * @return JwtUserDetails
     */
    public JwtUserDetails getJwtUserDetails(String token) {
        return JwtUserDetails.builder()
                .userId(getUserId(token))
                .username(getUsername(token))
                .role(getRole(token))
                .classId(getClassId(token))
                .build();
    }

    /**
     * 获取Access Token有效期（秒）
     */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    /**
     * 解析Token
     *
     * @param token Token字符串
     * @return Claims
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
