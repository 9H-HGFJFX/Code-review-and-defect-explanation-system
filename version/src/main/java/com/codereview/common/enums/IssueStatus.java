package com.codereview.common.enums;

/**
 * 缺陷状态枚举
 * 用于定义代码缺陷的当前处理状态
 */
public enum IssueStatus {
    /**
     * 新建/未处理
     */
    NEW(0, "NEW"),

    /**
     * 未处理/开放状态
     */
    OPEN(0, "OPEN"),

    /**
     * 已分配
     */
    ASSIGNED(1, "ASSIGNED"),

    /**
     * 已修复
     */
    RESOLVED(2, "RESOLVED"),

    /**
     * 已关闭
     */
    CLOSED(3, "CLOSED");

    private final int value;
    private final String name;

    IssueStatus(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public int getCode() {
        return value;
    }

    public String getName() {
        return name;
    }

    /**
     * 根据整数值获取枚举
     */
    public static IssueStatus fromValue(int value) {
        for (IssueStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid IssueStatus value: " + value);
    }

    public static IssueStatus fromCode(int code) {
        return fromValue(code);
    }

    public static IssueStatus fromName(String name) {
        try {
            return IssueStatus.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid IssueStatus name: " + name);
        }
    }
}