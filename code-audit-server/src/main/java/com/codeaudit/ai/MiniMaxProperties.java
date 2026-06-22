package com.codeaudit.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * MiniMax M3 API 配置。
 * Spring Boot 3 要求配置 key 是 kebab-case 小写；这里用 minimax 兼容现有 application.yml。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "minimax")
public class MiniMaxProperties {
    private String apiKey;
    private String baseUrl = "https://api.MiniMax.chat/v1";
    private String model = "MiniMax-M3";
    private boolean enabled = false;
    private Integer timeoutSeconds = 30;
}
