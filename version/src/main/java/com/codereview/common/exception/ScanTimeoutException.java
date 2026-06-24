package com.codereview.common.exception;

/**
 * 扫描超时异常
 * 代码扫描操作超时时抛出，错误码：9004，HTTP状态码：504
 */
public class ScanTimeoutException extends BaseException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private static final int CODE = 9004;

    /**
     * HTTP状态码
     */
    private static final int HTTP_STATUS = 504;

    /**
     * 构造函数
     *
     * @param message 异常消息
     */
    public ScanTimeoutException(String message) {
        super(CODE, HTTP_STATUS, message);
    }

    /**
     * 构造函数（包含原因）
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public ScanTimeoutException(String message, Throwable cause) {
        super(CODE, HTTP_STATUS, message, cause);
    }
}