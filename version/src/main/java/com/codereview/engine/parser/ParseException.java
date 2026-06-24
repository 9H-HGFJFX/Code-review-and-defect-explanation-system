package com.codereview.engine.parser;

/**
 * 解析异常类
 * 当代码解析失败时抛出
 * 
 * @author code-review-team
 */
public class ParseException extends Exception {
    
    /**
     * 异常类型
     */
    private final String errorType;
    
    /**
     * 出错的文件路径
     */
    private final String filePath;
    
    /**
     * 出错的行号
     */
    private final Integer lineNumber;

    public ParseException(String message) {
        super(message);
        this.errorType = "UNKNOWN";
        this.filePath = null;
        this.lineNumber = null;
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
        this.errorType = "UNKNOWN";
        this.filePath = null;
        this.lineNumber = null;
    }

    public ParseException(String message, String errorType, String filePath, Integer lineNumber) {
        super(message);
        this.errorType = errorType;
        this.filePath = filePath;
        this.lineNumber = lineNumber;
    }

    public ParseException(String message, Throwable cause, String errorType, String filePath) {
        super(message, cause);
        this.errorType = errorType;
        this.filePath = filePath;
        this.lineNumber = null;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getFilePath() {
        return filePath;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    /**
     * 创建语法错误异常
     */
    public static ParseException syntaxError(String message, String filePath, Integer lineNumber) {
        return new ParseException(message, "SYNTAX_ERROR", filePath, lineNumber);
    }

    /**
     * 创建编码错误异常
     */
    public static ParseException encodingError(String message, String filePath) {
        return new ParseException(message, "ENCODING_ERROR", filePath, null);
    }

    /**
     * 创建超时异常
     */
    public static ParseException timeoutError(String message, String filePath) {
        return new ParseException(message, "TIMEOUT", filePath, null);
    }

    /**
     * 创建文件过大异常
     */
    public static ParseException fileTooLarge(String message, String filePath) {
        return new ParseException(message, "FILE_TOO_LARGE", filePath, null);
    }
}
