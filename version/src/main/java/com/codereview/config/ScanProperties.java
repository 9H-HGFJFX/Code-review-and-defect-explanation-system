package com.codereview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 扫描配置属性类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "scan")
public class ScanProperties {

    /**
     * 扫描超时时间（秒）
     */
    private Integer timeout = 300;

    /**
     * 单个文件最大大小（字节）
     */
    private Long maxFileSize = 10485760L; // 10MB

    /**
     * 每个任务最大文件数
     */
    private Integer maxFilesPerTask = 1000;
}