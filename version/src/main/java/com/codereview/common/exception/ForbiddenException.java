package com.codereview.common.exception;

/**
 * 无权限异常
 * 用户已认证但无权限访问资源时抛出，错误码：1002，HTTP状态码：403
 */
public class ForbiddenException extends BaseException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private static final int CODE = 1002;

    /**
     * HTTP状态码
     */
    private static final int HTTP_STATUS = 403;

    /**
     * 构造函数
     *
     * @param message 异常消息
     */
    public ForbiddenException(String message) {
        super(CODE, HTTP_STATUS, message);
    }

    /**
     * 构造函数（自定义错误码和消息）
     *
     * @param code    错误码
     * @param message 异常消息
     */
    public ForbiddenException(int code, String message) {
        super(code, HTTP_STATUS, message);
    }

    /**
     * 构造函数（包含原因）
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public ForbiddenException(String message, Throwable cause) {
        super(CODE, HTTP_STATUS, message, cause);
    }
}