package com.codeaudit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "code-audit")
public class CodeAuditProperties {
    /** 单次最大代码行数（需求分析要求 ≤500） */
    private Integer maxLines = 500;
    /** 同步审查阈值 */
    private Integer syncMaxLines = 500;
    /** 异步审查阈值 */
    private Integer asyncMaxLines = 5000;
    /** 审查任务超时（毫秒） */
    private Long reviewTimeoutMs = 30000L;
    /** 审查任务线程池大小 */
    private Integer reviewPoolSize = 10;
}
