package com.codereview.common.enums;

/**
 * 规则加载失败模式枚举
 * 定义规则加载失败时的处理策略
 * 
 * @author code-review-team
 */
public enum FailMode {
    
    /**
     * 记录错误日志并跳过（默认）
     * 加载失败时跳过该规则并记录ERROR日志，不阻止扫描启动
     * 适用于生产环境
     */
    LOG_AND_SKIP("记录错误并跳过"),
    
    /**
     * 严格模式
     * 加载失败时抛出异常，中断启动流程
     * 适用于单元测试或严格校验场景
     */
    STRICT("严格模式"),
    
    /**
     * 记录警告并跳过
     * 加载失败时记录WARN日志并跳过
     * 适用于开发调试场景
     */
    WARN("记录警告并跳过");

    private final String description;

    FailMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
