package com.codereview.common.enums;

/**
 * 审查任务状态枚举
 * 用于定义代码审查任务的生命周期状态
 */
public enum TaskStatus {
    /**
     * 已创建/待处理
     */
    PENDING(0, "PENDING"),

    /**
     * 运行中/扫描中
     */
    RUNNING(1, "RUNNING"),

    /**
     * 已完成
     */
    COMPLETED(2, "COMPLETED"),

    /**
     * 部分完成
     */
    PARTIAL_COMPLETED(2, "PARTIAL_COMPLETED"),

    /**
     * 失败
     */
    FAILED(3, "FAILED"),

    /**
     * 已取消
     */
    CANCELLED(4, "CANCELLED");

    private final int value;
    private final String name;

    TaskStatus(int value, String name) {
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
    public static TaskStatus fromValue(int value) {
        for (TaskStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid TaskStatus value: " + value);
    }

    /**
     * 根据整数值获取枚举（别名）
     */
    public static TaskStatus fromCode(int code) {
        return fromValue(code);
    }

    /**
     * 根据名称字符串获取枚举
     */
    public static TaskStatus fromName(String name) {
        for (TaskStatus status : values()) {
            if (status.name.equalsIgnoreCase(name) || status.name().equalsIgnoreCase(name)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid TaskStatus name: " + name);
    }

    /**
     * 状态转换是否合法
     */
    public boolean canTransitionTo(TaskStatus target) {
        if (this == target) return true;
        switch (this) {
            case PENDING:
                return target == RUNNING || target == FAILED;
            case RUNNING:
                return target == COMPLETED || target == PARTIAL_COMPLETED || target == FAILED;
            case PARTIAL_COMPLETED:
                return target == COMPLETED || target == FAILED;
            case COMPLETED:
            case FAILED:
                return false;
            default:
                return false;
        }
    }
}