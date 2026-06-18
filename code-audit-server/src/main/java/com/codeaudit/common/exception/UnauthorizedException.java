package com.codeaudit.common.exception;

import com.codeaudit.common.Result;

/**
 * 未认证 / Token 过期
 */
public class UnauthorizedException extends BizException {
    public UnauthorizedException(String message) {
        super(Result.CODE_UNAUTHORIZED, message);
    }
}
