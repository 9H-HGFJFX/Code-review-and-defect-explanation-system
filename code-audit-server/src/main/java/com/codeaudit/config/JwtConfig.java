package com.codeaudit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    /** 签名密钥 */
    private String secret;
    /** AccessToken 有效期（秒） */
    private Long accessExpireSeconds = 7200L;
    /** RefreshToken 有效期（秒） */
    private Long refreshExpireSeconds = 604800L;
    /** HTTP Header 名 */
    private String header = "Authorization";
    /** Token 前缀 */
    private String prefix = "Bearer ";
}
