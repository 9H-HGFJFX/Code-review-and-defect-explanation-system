package com.codereview.common;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一响应结果封装
 */
@Data
public class Result<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 状态码 */
    private Integer code;
    
    /** 消息 */
    private String message;
    
    /** 数据 */
    private T data;
    
    /** 时间戳 */
    private Long timestamp;
    
    public Result() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
    
    // ==================== Success Methods ====================
    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }
    
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }
    
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }
    
    // ==================== Fail Methods ====================
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }
    
    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }
    
    // ==================== Business Error Codes ====================
    // Authentication errors: 10001-10099
    public static final Integer ERR_ACCOUNT_PASSWORD = 10001;
    public static final Integer ERR_TOKEN_EXPIRED = 10002;
    public static final Integer ERR_TOKEN_INVALID = 10003;
    public static final Integer ERR_ACCOUNT_LOCKED = 10004;
    public static final Integer ERR_NO_PERMISSION = 10005;
    
    // Review errors: 20001-20099
    public static final Integer ERR_CODE_LINES_EXCEED = 20001;
    public static final Integer ERR_CODE_SYNTAX_ERROR = 20002;
    public static final Integer ERR_REVIEW_TIMEOUT = 20003;
    public static final Integer ERR_REVIEW_NOT_FOUND = 20004;
    
    // Rule errors: 30001-30099
    public static final Integer ERR_RULE_NOT_FOUND = 30001;
    public static final Integer ERR_RULE_DUPLICATE = 30002;
    
    // Class errors: 40001-40099
    public static final Integer ERR_CLASS_NOT_FOUND = 40001;
    public static final Integer ERR_CLASS_FULL = 40002;
    public static final Integer ERR_USER_NOT_IN_CLASS = 40003;
}
