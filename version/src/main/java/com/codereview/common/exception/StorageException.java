package com.codereview.common.exception;

/**
 * 存储服务异常
 * 文件存储操作失败时抛出，错误码：9003，HTTP状态码：500
 */
public class StorageException extends BaseException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private static final int CODE = 9003;

    /**
     * HTTP状态码
     */
    private static final int HTTP_STATUS = 500;

    /**
     * 构造函数
     *
     * @param message 异常消息
     */
    public StorageException(String message) {
        super(CODE, HTTP_STATUS, message);
    }

    /**
     * 构造函数（包含原因）
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public StorageException(String message, Throwable cause) {
        super(CODE, HTTP_STATUS, message, cause);
    }
}