package com.codereview.common.annotation;

import com.codereview.common.enums.UserRole;

import java.lang.annotation.*;

/**
 * 权限注解
 * 用于标注需要特定角色才能访问的方法
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {

    /**
     * 允许访问的角色列表
     */
    UserRole[] roles() default {};

    /**
     * 权限名称（可选，用于日志记录）
     */
    String name() default "";
}
