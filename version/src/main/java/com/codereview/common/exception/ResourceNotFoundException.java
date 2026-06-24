package com.codereview.common.exception;

/**
 * 资源不存在异常
 * 请求的资源不存在时抛出，错误码：1003，HTTP状态码：404
 */
public class ResourceNotFoundException extends BaseException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private static final int CODE = 1003;

    /**
     * HTTP状态码
     */
    private static final int HTTP_STATUS = 404;

    /**
     * 构造函数
     *
     * @param message 异常消息
     */
    public ResourceNotFoundException(String message) {
        super(CODE, HTTP_STATUS, message);
    }

    /**
     * 构造函数（包含原因）
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(CODE, HTTP_STATUS, message, cause);
    }

    /**
     * 构造函数（指定资源类型和ID）
     *
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     */
    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super(CODE, HTTP_STATUS, String.format("%s with id '%s' not found", resourceType, resourceId));
    }
}