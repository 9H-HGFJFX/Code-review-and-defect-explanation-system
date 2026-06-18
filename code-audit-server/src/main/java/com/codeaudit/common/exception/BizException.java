package com.codeaudit.common.exception;

import com.codeaudit.common.Result;
import lombok.Getter;

/**
 * 业务异常基类
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = Result.CODE_SERVER_ERROR;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
