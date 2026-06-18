package com.codeaudit.common.exception;

import com.codeaudit.common.Result;

/**
 * 权限不足
 */
public class ForbiddenException extends BizException {
    public ForbiddenException(String message) {
        super(Result.CODE_FORBIDDEN, message);
    }
}
