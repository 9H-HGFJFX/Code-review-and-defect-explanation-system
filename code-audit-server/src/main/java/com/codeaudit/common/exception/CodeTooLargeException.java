package com.codeaudit.common.exception;

import com.codeaudit.common.Result;

/**
 * 代码行数超限
 */
public class CodeTooLargeException extends BizException {
    public CodeTooLargeException(int maxLines) {
        super(Result.BIZ_CODE_TOO_LARGE, "代码行数超出限制（≤" + maxLines + " 行），请精简后重新提交");
    }
}
