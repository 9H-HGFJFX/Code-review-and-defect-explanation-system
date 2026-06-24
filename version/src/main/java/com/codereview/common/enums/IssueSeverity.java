package com.codereview.common.enums;

/**
 * 缺陷严重程度枚举
 * 用于定义代码缺陷的严重程度等级
 */
public enum IssueSeverity {
    /**
     * 信息
     */
    INFO(0, "INFO", "信息提示"),

    /**
     * 严重 - 必须立即修复
     */
    CRITICAL(1, "CRITICAL", "严重"),

    /**
     * 高 - 应该在近期修复
     */
    HIGH(2, "HIGH", "高"),

    /**
     * 中 - 应该在合适的时间修复
     */
    MEDIUM(3, "MEDIUM", "中"),

    /**
     * 低 - 建议修复但不紧急
     */
    LOW(4, "LOW", "低");

    private final int value;
    private final String name;
    private final String description;

    IssueSeverity(int value, String name, String description) {
        this.value = value;
        this.name = name;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    /**
     * 根据整数值获取枚举
     */
    public static IssueSeverity fromValue(int value) {
        for (IssueSeverity severity : values()) {
            if (severity.value == value) {
                return severity;
            }
        }
        throw new IllegalArgumentException("Invalid IssueSeverity value: " + value);
    }

    public static IssueSeverity fromCode(int code) {
        return fromValue(code);
    }

    public static IssueSeverity fromName(String name) {
        try {
            return IssueSeverity.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid IssueSeverity name: " + name);
        }
    }
}