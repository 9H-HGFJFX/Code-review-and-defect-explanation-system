package com.codereview.common.enums;

/**
 * 用户角色枚举
 * 用于定义系统用户的角色类型
 */
public enum UserRole {
    /**
     * 超级管理员
     */
    SUPER_ADMIN(1),

    /**
     * 教师
     */
    TEACHER(2),

    /**
     * 学生
     */
    STUDENT(3);

    private final int value;

    UserRole(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public int getCode() {
        return value;
    }

    public static UserRole fromName(String name) {
        try {
            return UserRole.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UserRole name: " + name);
        }
    }

    /**
     * 根据整数值获取枚举
     */
    public static UserRole fromValue(int value) {
        for (UserRole role : values()) {
            if (role.value == value) {
                return role;
            }
        }
        throw new IllegalArgumentException("Invalid UserRole value: " + value);
    }

    /**
     * 根据整数值获取枚举（别名方法）
     */
    public static UserRole fromCode(int code) {
        return fromValue(code);
    }

    /**
     * 判断是否为管理员角色
     */
    public boolean isAdmin() {
        return this == SUPER_ADMIN;
    }

    /**
     * 判断是否为教师角色
     */
    public boolean isTeacher() {
        return this == TEACHER;
    }

    /**
     * 判断是否为学生角色
     */
    public boolean isStudent() {
        return this == STUDENT;
    }

    /**
     * 是否可以创建任务
     */
    public boolean canCreateTask() {
        return this == TEACHER;
    }

    /**
     * 是否可以删除任务
     */
    public boolean canDeleteTask() {
        return this == SUPER_ADMIN;
    }

    /**
     * 是否可以管理规则
     */
    public boolean canManageRules() {
        return this == SUPER_ADMIN;
    }

    /**
     * 是否可以管理班级
     */
    public boolean canManageClass() {
        return this == SUPER_ADMIN;
    }
}