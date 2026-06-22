package com.codereview.exception;

/**
 * 代码行数超限异常
 */
public class CodeLengthExceededException extends BusinessException {
    
    public CodeLengthExceededException(int actualLines, int maxLines) {
        super(20001, String.format("代码行数%d超出限制%d行，拒绝审查", actualLines, maxLines));
    }
}
