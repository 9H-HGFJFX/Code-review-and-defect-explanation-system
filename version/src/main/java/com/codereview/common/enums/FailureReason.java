package com.codereview.common.enums;

/**
 * 文件解析失败原因枚举
 * 定义文件解析失败的具体原因
 * 
 * @author code-review-team
 */
public enum FailureReason {
    
    /**
     * 语法解析错误
     * 文件包含语法错误，无法解析为有效的AST
     */
    PARSE_ERROR("语法解析错误"),
    
    /**
     * 解析超时
     * 文件过大或复杂，解析超过超时限制
     */
    TIMEOUT("解析超时"),
    
    /**
     * 编码错误
     * 无法识别文件的字符编码
     */
    ENCODING_ERROR("编码错误"),
    
    /**
     * 文件过大
     * 文件大小超过系统限制
     */
    FILE_TOO_LARGE("文件过大"),
    
    /**
     * 未知错误
     * 其他未分类的错误
     */
    UNKNOWN_ERROR("未知错误");

    private final String description;

    FailureReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
