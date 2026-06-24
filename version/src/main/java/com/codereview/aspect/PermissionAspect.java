package com.codereview.aspect;

import com.codereview.common.annotation.RequiresPermission;
import com.codereview.common.enums.ErrorCode;
import com.codereview.common.enums.UserRole;
import com.codereview.security.JwtUserDetails;
import com.codereview.security.JwtUserDetailsAdapter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 权限检查切面
 * 拦截带有@RequiresPermission注解的方法，进行角色权限校验
 */
@Slf4j
@Aspect
@Component
public class PermissionAspect {

    /**
     * 在方法执行前检查权限
     */
    @Before("@annotation(com.codereview.common.annotation.RequiresPermission)")
    public void checkPermission(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresPermission annotation = method.getAnnotation(RequiresPermission.class);

        if (annotation == null) {
            // 检查类级别注解
            Class<?> clazz = method.getDeclaringClass();
            annotation = clazz.getAnnotation(RequiresPermission.class);
        }

        if (annotation == null) {
            return;
        }

        // 获取当前用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new com.codereview.common.exception.UnauthorizedException();
        }

        if (!(authentication.getPrincipal() instanceof JwtUserDetailsAdapter userDetails)) {
            throw new com.codereview.common.exception.UnauthorizedException();
        }

        JwtUserDetails jwtUser = userDetails.getJwtUserDetails();
        UserRole userRole = jwtUser.getRole();

        // 检查角色权限
        UserRole[] allowedRoles = annotation.roles();
        boolean hasPermission = false;

        for (UserRole allowedRole : allowedRoles) {
            if (allowedRole == userRole) {
                hasPermission = true;
                break;
            }
        }

        if (!hasPermission) {
            log.warn("Permission denied: userId={}, role={}, requiredRoles={}, method={}",
                    jwtUser.getUserId(), userRole, annotation.roles(), method.getName());

            throw new com.codereview.common.exception.ForbiddenException(
                    ErrorCode.PERMISSION_DENIED.getCode(),
                    ErrorCode.PERMISSION_DENIED.getMessage()
            );
        }

        log.debug("Permission granted: userId={}, role={}, method={}",
                jwtUser.getUserId(), userRole, method.getName());
    }
}
