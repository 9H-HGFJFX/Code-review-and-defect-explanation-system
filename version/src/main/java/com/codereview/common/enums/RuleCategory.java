package com.codereview.common.enums;

/**
 * 规则分类枚举
 * 用于定义代码审查规则的分类
 */
public enum RuleCategory {
    /**
     * 代码风格
     */
    STYLE(1),

    /**
     * 安全问题
     */
    SECURITY(2),

    /**
     * 性能问题
     */
    PERFORMANCE(3),

    /**
     * 最佳实践
     */
    BEST_PRACTICE(4),

    /**
     * 正确性
     */
    CORRECTNESS(5);

    private final int value;

    RuleCategory(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public int getCode() {
        return value;
    }

    public String getName() {
        return name();
    }

    /**
     * 根据整数值获取枚举
     */
    public static RuleCategory fromValue(int value) {
        for (RuleCategory category : values()) {
            if (category.value == value) {
                return category;
            }
        }
        throw new IllegalArgumentException("Invalid RuleCategory value: " + value);
    }

    public static RuleCategory fromCode(int code) {
        return fromValue(code);
    }

    public static RuleCategory fromName(String name) {
        try {
            return RuleCategory.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid RuleCategory name: " + name);
        }
    }
}