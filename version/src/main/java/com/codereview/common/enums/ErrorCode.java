package com.codereview.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务错误码枚举
 * 遵循 worker4-api.md 第5.1.3节的错误码体系
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 成功
    SUCCESS(0, "success"),

    // 认证授权类 1000-1999
    UNAUTHORIZED(1000, "unauthorized"),
    TOKEN_EXPIRED(1001, "token expired"),
    FORBIDDEN(1002, "forbidden"),
    TOKEN_INVALID(1003, "token invalid"),
    TOKEN_MISSING(1004, "token missing"),
    PERMISSION_DENIED(1005, "permission denied"),
    ROLE_FORBIDDEN(1006, "role forbidden"),
    REFRESH_TOKEN_EXPIRED(1007, "refresh token expired"),
    USER_DISABLED(1008, "user disabled"),

    // 审查任务类 2000-2999
    TASK_CREATE_FAILED(2001, "task create failed"),
    TASK_NOT_FOUND(2002, "task not found"),
    TASK_STATUS_INVALID(2003, "task status invalid"),
    TASK_NOT_IN_CLASS(2004, "task not in class"),

    // 缺陷管理类 3000-3999
    ISSUE_NOT_FOUND(3001, "issue not found"),
    ISSUE_ASSIGN_FAILED(3002, "issue assign failed"),
    ISSUE_STATUS_INVALID(3003, "issue status invalid"),
    ISSUE_NOT_ASSIGNABLE(3004, "issue not assignable"),

    // 规则管理类 4000-4999
    RULE_CREATE_FAILED(4001, "rule create failed"),
    RULE_NOT_FOUND(4002, "rule not found"),
    RULE_UPDATE_FAILED(4003, "rule update failed"),

    // 班级管理类 5000-5999
    CLASS_NOT_FOUND(5001, "class not found"),
    CLASS_CREATE_FAILED(5002, "class create failed"),
    STUDENT_NOT_FOUND(5003, "student not found"),

    // 系统类 9000-9999
    CIRCUIT_BREAKER_OPEN(9001, "circuit breaker open"),
    EXTERNAL_SERVICE_ERROR(9002, "external service error"),
    INTERNAL_ERROR(9003, "internal error"),
    SERVICE_UNAVAILABLE(9004, "service unavailable"),
    RATE_LIMIT_EXCEEDED(9005, "rate limit exceeded"),
    DATABASE_ERROR(9006, "database error");

    /**
     * 错误码
     */
    private final int code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 根据错误码获取枚举
     */
    public static ErrorCode fromCode(int code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.code == code) {
                return errorCode;
            }
        }
        return INTERNAL_ERROR;
    }
}
