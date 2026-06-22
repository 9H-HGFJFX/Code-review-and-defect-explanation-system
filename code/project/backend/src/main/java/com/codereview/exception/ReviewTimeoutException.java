package com.codereview.exception;

/**
 * 审查超时异常
 */
public class ReviewTimeoutException extends BusinessException {
    
    public ReviewTimeoutException() {
        super(20003, "代码审查超时，请尝试减少代码行数或稍后重试");
    }
    
    public ReviewTimeoutException(int timeoutSeconds) {
        super(20003, String.format("代码审查超过%d秒，已自动终止", timeoutSeconds));
    }
}
