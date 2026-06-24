package com.codereview.common.exception;

import com.codereview.common.enums.ErrorCode;

/**
 * 业务异常类
 * 用于业务校验失败等场景，错误码范围：1001-1999
 */
public class BusinessException extends BaseException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码前缀（业务异常范围：1001-1999）
     */
    private static final int BASE_CODE = 1001;

    /**
     * 构造函数
     *
     * @param message 异常消息
     */
    public BusinessException(String message) {
        super(BASE_CODE, 400, message);
    }

    /**
     * 构造函数（自定义错误码）
     *
     * @param code    错误码
     * @param message 异常消息
     */
    public BusinessException(int code, String message) {
        super(code, 400, message);
    }

    /**
     * 构造函数（错误码枚举）
     *
     * @param errorCode 错误码枚举
     * @param message   异常消息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode.getCode(), 400, message);
    }

    /**
     * 构造函数（错误码枚举，仅消息）
     *
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getCode(), 400, errorCode.getMessage());
    }

    /**
     * 构造函数（包含原因）
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public BusinessException(String message, Throwable cause) {
        super(BASE_CODE, 400, message, cause);
    }

    /**
     * 构造函数（自定义错误码和原因）
     *
     * @param code    错误码
     * @param message 异常消息
     * @param cause   异常原因
     */
    public BusinessException(int code, String message, Throwable cause) {
        super(code, 400, message, cause);
    }

    /**
     * 构造函数（错误码枚举和原因）
     *
     * @param errorCode 错误码枚举
     * @param message   异常消息
     * @param cause     异常原因
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode.getCode(), 400, message, cause);
    }
}