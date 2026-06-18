package com.codeaudit.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * 网关侧 JWT 工具
 * 仅做"验证"业务侧签发的 token，不生成新 token
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    public Long getUserId(Claims claims) {
        Object v = claims.get("uid");
        return v == null ? null : Long.valueOf(v.toString());
    }

    public String getUsername(Claims claims) {
        Object v = claims.get("uname");
        return v == null ? null : v.toString();
    }

    public String getRole(Claims claims) {
        Object v = claims.get("role");
        return v == null ? null : v.toString();
    }

    public String getType(Claims claims) {
        Object v = claims.get("type");
        return v == null ? null : v.toString();
    }
}
