package com.codereview.common.exception;

/**
 * 外部服务异常
 * 调用外部服务失败时抛出，错误码：9002，HTTP状态码：502
 */
public class ExternalServiceException extends BaseException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private static final int CODE = 9002;

    /**
     * HTTP状态码
     */
    private static final int HTTP_STATUS = 502;

    /**
     * 构造函数
     *
     * @param message 异常消息
     */
    public ExternalServiceException(String message) {
        super(CODE, HTTP_STATUS, message);
    }

    /**
     * 构造函数（包含原因）
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public ExternalServiceException(String message, Throwable cause) {
        super(CODE, HTTP_STATUS, message, cause);
    }
}