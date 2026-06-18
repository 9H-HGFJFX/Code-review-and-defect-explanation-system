package com.codeaudit.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应格式
 * 成功：{"code":200,"message":"success","data":{...}}
 * 失败：{"code":4xx/5xx,"message":"错误描述","data":null}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final int CODE_SUCCESS = 200;
    public static final int CODE_BAD_REQUEST = 400;
    public static final int CODE_UNAUTHORIZED = 401;
    public static final int CODE_FORBIDDEN = 403;
    public static final int CODE_NOT_FOUND = 404;
    public static final int CODE_SERVER_ERROR = 500;

    /** 业务错误码：10001-账号密码错误 10002-Token过期 20001-代码行数超限 20002-代码语法错误 */
    public static final int BIZ_AUTH_FAIL = 10001;
    public static final int BIZ_TOKEN_EXPIRED = 10002;
    public static final int BIZ_CODE_TOO_LARGE = 20001;
    public static final int BIZ_CODE_PARSE_FAIL = 20002;

    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success() {
        return new Result<>(CODE_SUCCESS, "success", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(CODE_SUCCESS, "success", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(CODE_SUCCESS, message, data);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(CODE_SERVER_ERROR, message, null);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> bizError(int code, String message) {
        return new Result<>(code, message, null);
    }
}
