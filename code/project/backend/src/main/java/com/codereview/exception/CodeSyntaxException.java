package com.codereview.exception;

/**
 * 代码语法错误异常
 */
public class CodeSyntaxException extends BusinessException {
    
    public CodeSyntaxException(String message) {
        super(20002, "Java代码语法错误，无法解析: " + message);
    }
}
