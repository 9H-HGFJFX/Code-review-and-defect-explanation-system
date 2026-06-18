package com.codeaudit.common.exception;

import com.codeaudit.common.Result;

/**
 * 代码语法解析失败
 */
public class CodeParseException extends BizException {
    public CodeParseException(String detail) {
        super(Result.BIZ_CODE_PARSE_FAIL, "Java 代码语法错误，无法解析: " + detail);
    }
}
