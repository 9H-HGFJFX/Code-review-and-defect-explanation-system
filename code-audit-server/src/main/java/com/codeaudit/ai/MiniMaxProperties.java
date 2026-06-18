package com.codeaudit.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * MiniMax M3 API 配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "MiniMax")
public class MiniMaxProperties {
    private String apiKey;
    private String baseUrl = "https://api.MiniMax.chat/v1";
    private String model = "MiniMax-M3";
    private boolean enabled = false;
    private Integer timeoutSeconds = 30;
}
