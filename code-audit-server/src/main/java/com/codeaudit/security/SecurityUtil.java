package com.codeaudit.security;

import com.codeaudit.common.exception.ForbiddenException;
import com.codeaudit.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 当前登录用户工具
 *
 * V1.0 适配网关层：优先从 X-User-* 头读取（由网关解析 JWT 后注入），
 * 兜底从 Spring SecurityContext 读取（用于本地直连业务服务的开发场景）。
 */
public final class SecurityUtil {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_NAME = "X-User-Name";
    public static final String HEADER_USER_ROLE = "X-User-Role";

    private SecurityUtil() {}

    /**
     * 优先从 HTTP header 读取，兜底从 SecurityContext 读取
     */
    public static LoginUser currentLoginUser() {
        Long uid = headerUserId();
        if (uid != null) {
            String name = headerUserName() != null ? headerUserName() : "";
            String role = headerUserRole() != null ? headerUserRole() : "STUDENT";
            return new LoginUser(uid, name, "", role, 1);
        }
        // 兜底：本地开发场景
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof LoginUser lu)) {
            throw new UnauthorizedException("未登录或登录已过期");
        }
        return lu;
    }

    public static Long currentUserId() {
        Long uid = headerUserId();
        if (uid != null) return uid;
        return currentLoginUser().getId();
    }

    public static String currentUsername() {
        String name = headerUserName();
        if (name != null) return name;
        return currentLoginUser().getUsername();
    }

    public static String currentRole() {
        String role = headerUserRole();
        if (role != null) return role;
        return currentLoginUser().getRole();
    }

    public static void requireRole(String... roles) {
        String r = currentRole();
        for (String role : roles) {
            if (role.equalsIgnoreCase(r)) return;
        }
        throw new ForbiddenException("权限不足");
    }

    // -------- 私有 header 工具 --------

    private static HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs == null ? null : attrs.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private static Long headerUserId() {
        HttpServletRequest req = currentRequest();
        if (req == null) return null;
        String v = req.getHeader(HEADER_USER_ID);
        if (v == null || v.isBlank()) return null;
        try { return Long.valueOf(v); } catch (NumberFormatException e) { return null; }
    }

    private static String headerUserName() {
        HttpServletRequest req = currentRequest();
        return req == null ? null : req.getHeader(HEADER_USER_NAME);
    }

    private static String headerUserRole() {
        HttpServletRequest req = currentRequest();
        return req == null ? null : req.getHeader(HEADER_USER_ROLE);
    }
}
