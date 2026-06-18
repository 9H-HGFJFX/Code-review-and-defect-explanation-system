package com.codeaudit.common.exception;

import com.codeaudit.common.Result;
import com.codeaudit.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * 全局异常拦截器 - 概要设计 7.2
 * 统一格式返回，前端无需处理各类后端报错
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBiz(BizException ex, HttpServletRequest req) {
        log.warn("[BIZ] {} {} -> code={}, msg={}", req.getMethod(), req.getRequestURI(), ex.getCode(), ex.getMessage());
        return ResponseEntity.ok(Result.error(ex.getCode(), ex.getMessage()));
    }

    /** 参数校验 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("[VALIDATION] {}", msg);
        return ResponseEntity.ok(Result.error(Result.CODE_BAD_REQUEST, msg));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBind(BindException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.ok(Result.error(Result.CODE_BAD_REQUEST, msg));
    }

    /** Spring Security 鉴权失败 */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuth(AuthenticationException ex) {
        log.warn("[AUTH] {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.error(Result.BIZ_AUTH_FAIL, "认证失败: " + ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccess(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.error(Result.CODE_FORBIDDEN, "权限不足"));
    }

    /** 文件上传过大 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleUpload(MaxUploadSizeExceededException ex) {
        return ResponseEntity.ok(Result.error(Result.CODE_BAD_REQUEST, "上传文件超过限制"));
    }

    /** 兜底未知异常 - 屏蔽堆栈 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleAll(Exception ex, HttpServletRequest req) {
        log.error("[UNKNOWN] {} {}", req.getMethod(), req.getRequestURI(), ex);
        return ResponseEntity.ok(Result.error(Result.CODE_SERVER_ERROR, "服务内部错误，请稍后重试"));
    }
}
