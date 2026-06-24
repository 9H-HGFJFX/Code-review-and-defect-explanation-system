package com.codereview.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.codereview.security.JwtUserDetails;
import com.codereview.security.JwtUserDetailsAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 数据范围过滤器
 * 根据用户角色自动注入数据隔离条件
 * STUDENT: 自动附加 classId = JWT.classId
 * TEACHER: 自动附加 classId = JWT.classId
 * SUPER_ADMIN: 不过滤
 */
@Slf4j
@Component
public class DataScopeFilter {

    /**
     * 获取当前用户的班级ID过滤条件
     *
     * @return 班级ID，如果为SUPER_ADMIN则返回null（不过滤）
     */
    public Long getClassIdFilter() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        if (authentication.getPrincipal() instanceof JwtUserDetailsAdapter userDetails) {
            JwtUserDetails jwtUser = userDetails.getJwtUserDetails();

            // SUPER_ADMIN不过滤，可以访问所有班级数据
            if (jwtUser.isAdmin()) {
                return null;
            }

            // TEACHER和STUDENT只能访问自己班级数据
            return jwtUser.getClassId();
        }

        return null;
    }

    /**
     * 检查当前用户是否为SUPER_ADMIN
     */
    public boolean isSuperAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof JwtUserDetailsAdapter userDetails) {
            return userDetails.getJwtUserDetails().isAdmin();
        }

        return false;
    }

    /**
     * 检查当前用户是否为TEACHER
     */
    public boolean isTeacher() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof JwtUserDetailsAdapter userDetails) {
            return userDetails.getJwtUserDetails().isTeacher();
        }

        return false;
    }

    /**
     * 检查当前用户是否为学生
     */
    public boolean isStudent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof JwtUserDetailsAdapter userDetails) {
            return userDetails.getJwtUserDetails().isStudent();
        }

        return false;
    }

    /**
     * 获取当前用户ID
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof JwtUserDetailsAdapter userDetails) {
            return userDetails.getJwtUserDetails().getUserId();
        }

        return null;
    }
}
