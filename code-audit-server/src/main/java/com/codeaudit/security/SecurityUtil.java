package com.codeaudit.security;

import com.codeaudit.common.exception.ForbiddenException;
import com.codeaudit.common.exception.UnauthorizedException;
import com.codeaudit.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户工具
 */
public final class SecurityUtil {

    private SecurityUtil() {}

    public static LoginUser currentLoginUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof LoginUser lu)) {
            throw new UnauthorizedException("未登录或登录已过期");
        }
        return lu;
    }

    public static Long currentUserId() {
        return currentLoginUser().getId();
    }

    public static String currentUsername() {
        return currentLoginUser().getUsername();
    }

    public static String currentRole() {
        return currentLoginUser().getRole();
    }

    public static void requireRole(String... roles) {
        String r = currentRole();
        for (String role : roles) {
            if (role.equalsIgnoreCase(r)) return;
        }
        throw new ForbiddenException("权限不足");
    }
}
