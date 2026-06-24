package com.codereview.engine.parser;

import lombok.Data;

/**
 * 解析结果封装类
 * 包含解析后的AST信息和可能的解析错误
 * 
 * @author code-review-team
 */
@Data
public class ParseResult {
    
    /**
     * 是否解析成功
     */
    private boolean success;
    
    /**
     * AST根节点（序列化形式）
     */
    private String astJson;
    
    /**
     * 解析失败时的错误信息
     */
    private String errorMessage;
    
    /**
     * 解析失败时的错误类型
     */
    private String errorType;
    
    /**
     * 文件行数
     */
    private Integer lineCount;
    
    /**
     * 提取的方法列表
     */
    private java.util.List<MethodInfo> methods;
    
    /**
     * 提取的类列表
     */
    private java.util.List<ClassInfo> classes;
    
    /**
     * 提取的导入语句
     */
    private java.util.List<String> imports;

    /**
     * 创建成功结果
     */
    public static ParseResult success() {
        ParseResult result = new ParseResult();
        result.setSuccess(true);
        return result;
    }

    /**
     * 创建成功结果
     */
    public static ParseResult success(String astJson) {
        ParseResult result = new ParseResult();
        result.setSuccess(true);
        result.setAstJson(astJson);
        return result;
    }

    /**
     * 创建失败结果
     */
    public static ParseResult failure(String errorMessage, String errorType) {
        ParseResult result = new ParseResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        result.setErrorType(errorType);
        return result;
    }

    /**
     * 方法信息内部类
     */
    @Data
    public static class MethodInfo {
        private String name;
        private int startLine;
        private int endLine;
        private int parameterCount;
        private String returnType;
        private int lineCount;
    }

    /**
     * 类信息内部类
     */
    @Data
    public static class ClassInfo {
        private String name;
        private int startLine;
        private int endLine;
        private int methodCount;
        private String packageName;
    }
}
