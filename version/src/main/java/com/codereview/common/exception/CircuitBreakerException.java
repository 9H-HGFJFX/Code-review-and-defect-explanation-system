package com.codereview.common.exception;

/**
 * 熔断异常
 * 当服务熔断器触发时抛出，错误码：9001，HTTP状态码：503
 */
public class CircuitBreakerException extends BaseException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private static final int CODE = 9001;

    /**
     * HTTP状态码
     */
    private static final int HTTP_STATUS = 503;

    /**
     * 构造函数
     *
     * @param message 异常消息
     */
    public CircuitBreakerException(String message) {
        super(CODE, HTTP_STATUS, message);
    }

    /**
     * 构造函数（包含原因）
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public CircuitBreakerException(String message, Throwable cause) {
        super(CODE, HTTP_STATUS, message, cause);
    }
}