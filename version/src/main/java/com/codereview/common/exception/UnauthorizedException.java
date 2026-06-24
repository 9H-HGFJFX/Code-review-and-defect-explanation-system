package com.codereview.common.exception;

import com.codereview.common.enums.ErrorCode;

/**
 * 未认证异常
 * 用户未登录或Token无效时抛出，错误码：1000，HTTP状态码：401
 */
public class UnauthorizedException extends BaseException {

    private static final long serialVersionUID = 1L;

    /**
     * HTTP状态码
     */
    private static final int HTTP_STATUS = 401;

    /**
     * 默认构造函数
     */
    public UnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED.getCode(), HTTP_STATUS, ErrorCode.UNAUTHORIZED.getMessage());
    }

    /**
     * 构造函数
     *
     * @param message 异常消息
     */
    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED.getCode(), HTTP_STATUS, message);
    }

    /**
     * 构造函数（自定义错误码）
     *
     * @param errorCode 错误码枚举
     */
    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode.getCode(), HTTP_STATUS, errorCode.getMessage());
    }

    /**
     * 构造函数（包含原因）
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public UnauthorizedException(String message, Throwable cause) {
        super(ErrorCode.UNAUTHORIZED.getCode(), HTTP_STATUS, message, cause);
    }
}