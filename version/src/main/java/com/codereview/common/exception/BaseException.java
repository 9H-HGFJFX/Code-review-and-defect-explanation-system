package com.codereview.common.exception;

import lombok.Getter;

/**
 * 基础异常类
 * 所有业务异常的父类，定义了错误码和HTTP状态码的统一格式
 */
@Getter
public class BaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final int code;

    /**
     * HTTP状态码
     */
    private final int httpStatus;

    /**
     * 构造函数
     *
     * @param code      错误码
     * @param httpStatus HTTP状态码
     * @param message   异常消息
     */
    public BaseException(int code, int httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    /**
     * 构造函数（包含原因）
     *
     * @param code      错误码
     * @param httpStatus HTTP状态码
     * @param message   异常消息
     * @param cause     异常原因
     */
    public BaseException(int code, int httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
    }
}