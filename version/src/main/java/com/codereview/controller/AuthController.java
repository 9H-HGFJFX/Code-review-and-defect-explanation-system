package com.codereview.controller;

import com.codereview.common.annotation.RequiresPermission;
import com.codereview.common.enums.UserRole;
import com.codereview.common.result.Result;
import com.codereview.dto.LoginRequest;
import com.codereview.dto.LoginResponse;
import com.codereview.dto.RefreshRequest;
import com.codereview.dto.RefreshResponse;
import com.codereview.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 处理用户登录、Token刷新、登出等认证相关请求
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     * 公开接口，无需认证
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.debug("Login request: username={}", request.getUsername());
        LoginResponse response = authService.login(request);
        return Result.success(response, "login success");
    }

    /**
     * 刷新Access Token
     * 需要已登录（携带有效的Refresh Token）
     */
    @PostMapping("/refresh")
    public Result<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        log.debug("Refresh token request received");
        RefreshResponse response = authService.refresh(request);
        return Result.success(response, "token refreshed");
    }

    /**
     * 用户登出
     * 需要已登录
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        // 从请求属性中获取Refresh Token（由过滤器设置）
        String refreshToken = (String) request.getAttribute("refreshToken");
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        return Result.success("logout success");
    }
}
