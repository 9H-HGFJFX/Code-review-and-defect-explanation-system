package com.codereview.exception;

import com.codereview.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 业务异常处理
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.error("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }
    
    /**
     * 参数校验异常处理
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        return Result.fail(400, message);
    }
    
    /**
     * 绑定异常处理
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定失败: {}", message);
        return Result.fail(400, message);
    }
    
    /**
     * 代码行数超限异常
     */
    @ExceptionHandler(CodeLengthExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleCodeLengthExceededException(CodeLengthExceededException e) {
        log.warn("代码行数超限: {}", e.getMessage());
        return Result.fail(20001, e.getMessage());
    }
    
    /**
     * 代码语法错误异常
     */
    @ExceptionHandler(CodeSyntaxException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleCodeSyntaxException(CodeSyntaxException e) {
        log.warn("代码语法错误: {}", e.getMessage());
        return Result.fail(20002, e.getMessage());
    }
    
    /**
     * 审查超时异常
     */
    @ExceptionHandler(ReviewTimeoutException.class)
    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    public Result<?> handleReviewTimeoutException(ReviewTimeoutException e) {
        log.warn("审查超时: {}", e.getMessage());
        return Result.fail(20003, e.getMessage());
    }
    
    /**
     * 其他未知异常处理
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.fail(500, "系统繁忙，请稍后重试");
    }
}
