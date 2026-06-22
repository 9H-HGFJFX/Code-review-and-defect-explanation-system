package com.codereview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置属性类
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    
    /** JWT签名密钥 */
    private String secret;
    
    /** Access Token有效期（毫秒） */
    private Long accessTokenExpiration;
    
    /** Refresh Token有效期（毫秒） */
    private Long refreshTokenExpiration;
}
