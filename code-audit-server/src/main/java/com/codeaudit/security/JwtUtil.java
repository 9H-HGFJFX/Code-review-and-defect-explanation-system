package com.codeaudit.security;

import com.codeaudit.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JWT 工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_USERNAME = "uname";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    /** Redis 黑名单 Key 前缀 */
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final JwtConfig jwtConfig;
    private final RedisTemplate<String, Object> redisTemplate;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /** 生成 AccessToken */
    public String generateAccessToken(Long userId, String username, String role) {
        return generate(userId, username, role, TYPE_ACCESS, jwtConfig.getAccessExpireSeconds() * 1000);
    }

    /** 生成 RefreshToken */
    public String generateRefreshToken(Long userId, String username, String role) {
        return generate(userId, username, role, TYPE_REFRESH, jwtConfig.getRefreshExpireSeconds() * 1000);
    }

    private String generate(Long userId, String username, String role, String type, long expireMs) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_USERNAME, username);
        claims.put(CLAIM_ROLE, role);
        claims.put(CLAIM_TOKEN_TYPE, type);
        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMs))
                .signWith(getKey())
                .compact();
    }

    /** 解析 Token */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    public Long getUserId(Claims claims) {
        Object v = claims.get(CLAIM_USER_ID);
        return v == null ? null : Long.valueOf(v.toString());
    }

    public String getUsername(Claims claims) {
        Object v = claims.get(CLAIM_USERNAME);
        return v == null ? null : v.toString();
    }

    public String getRole(Claims claims) {
        Object v = claims.get(CLAIM_ROLE);
        return v == null ? null : v.toString();
    }

    public String getType(Claims claims) {
        Object v = claims.get(CLAIM_TOKEN_TYPE);
        return v == null ? null : v.toString();
    }

    /** 把退出的 Token 加入黑名单 */
    public void blacklist(String token) {
        if (token == null) return;
        Claims claims = parse(token);
        if (claims == null) return;
        long expire = claims.getExpiration().getTime() - System.currentTimeMillis();
        if (expire <= 0) return;
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "1", expire, TimeUnit.MILLISECONDS);
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}
