package com.codereview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT配置属性类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * JWT密钥
     */
    private String secret;

    /**
     * 访问令牌有效期（秒）
     */
    private Long accessTokenValidity;

    /**
     * 刷新令牌有效期（秒）
     */
    private Long refreshTokenValidity;
}