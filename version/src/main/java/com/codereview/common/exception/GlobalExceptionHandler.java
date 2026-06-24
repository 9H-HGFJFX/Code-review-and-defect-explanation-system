package com.codereview.common.exception;

import com.codereview.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理所有Controller抛出的异常，返回标准化的错误响应
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理自定义基础异常
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<Result<Void>> handleBaseException(BaseException e, HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        log.error("RequestId: {}, Exception Code: {}, Message: {}", requestId, e.getCode(), e.getMessage(), e);

        Result<Void> result = Result.error(e.getCode(), e.getMessage());
        return ResponseEntity.status(e.getHttpStatus()).body(result);
    }

    /**
     * 处理业务校验异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        log.warn("RequestId: {}, Business Exception: {}", requestId, e.getMessage());

        Result<Void> result = Result.error(e.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 处理未认证异常
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Result<Void>> handleUnauthorizedException(UnauthorizedException e, HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        log.warn("RequestId: {}, Unauthorized: {}", requestId, e.getMessage());

        Result<Void> result = Result.error(e.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
    }

    /**
     * 处理无权限异常
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Result<Void>> handleForbiddenException(ForbiddenException e, HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        log.warn("RequestId: {}, Forbidden: {}", requestId, e.getMessage());

        Result<Void> result = Result.error(e.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
    }

    /**
     * 处理资源不存在异常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Result<Void>> handleResourceNotFoundException(ResourceNotFoundException e, HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        log.warn("RequestId: {}, Resource Not Found: {}", requestId, e.getMessage());

        Result<Void> result = Result.error(e.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }

    /**
     * 处理熔断异常
     */
    @ExceptionHandler(CircuitBreakerException.class)
    public ResponseEntity<Result<Void>> handleCircuitBreakerException(CircuitBreakerException e, HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        log.error("RequestId: {}, Circuit Breaker: {}", requestId, e.getMessage(), e);

        Result<Void> result = Result.error(e.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
    }

    /**
     * 处理外部服务异常
     */
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<Result<Void>> handleExternalServiceException(ExternalServiceException e, HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        log.error("RequestId: {}, External Service Error: {}", requestId, e.getMessage(), e);

        Result<Void> result = Result.error(e.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(result);
    }

    /**
     * 处理存储服务异常
     */
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<Result<Void>> handleStorageException(StorageException e, HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        log.error("RequestId: {}, Storage Error: {}", requestId, e.getMessage(), e);

        Result<Void> result = Result.error(e.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    /**
     * 处理扫描超时异常
     */
    @ExceptionHandler(ScanTimeoutException.class)
    public ResponseEntity<Result<Void>> handleScanTimeoutException(ScanTimeoutException e, HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        log.error("RequestId: {}, Scan Timeout: {}", requestId, e.getMessage(), e);

        Result<Void> result = Result.error(e.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(result);
    }

    /**
     * 处理参数校验异常（@Valid校验失败）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", errors);

        Result<Void> result = Result.error(1001, "参数校验失败: " + errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException e) {
        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Binding failed: {}", errors);

        Result<Void> result = Result.error(1001, "参数绑定失败: " + errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 处理所有未捕获的异常（最后兜底）
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e, HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        log.error("RequestId: {}, Unexpected Exception: {}", requestId, e.getMessage(), e);

        // 生产环境不暴露详细错误信息
        Result<Void> result = Result.error(5000, "系统内部错误，请稍后重试");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }
}