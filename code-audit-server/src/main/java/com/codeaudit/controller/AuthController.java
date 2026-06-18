package com.codeaudit.controller;

import com.codeaudit.common.Result;
import com.codeaudit.dto.LoginReq;
import com.codeaudit.dto.RegisterReq;
import com.codeaudit.entity.User;
import com.codeaudit.service.UserService;
import com.codeaudit.vo.LoginResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<LoginResp> register(@RequestBody @Valid RegisterReq req) {
        return Result.success(userService.register(req));
    }

    @PostMapping("/login")
    @Operation(summary = "登录")
    public Result<LoginResp> login(@RequestBody @Valid LoginReq req) {
        return Result.success(userService.login(req));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新 Token")
    public Result<LoginResp> refresh(@RequestParam String refreshToken) {
        return Result.success(userService.refreshToken(refreshToken));
    }

    @PostMapping("/logout")
    @Operation(summary = "登出（AccessToken 加入黑名单）")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            userService.logout(authHeader.substring(7));
        }
        return Result.success();
    }

    @GetMapping("/me")
    @Operation(summary = "当前用户信息")
    public Result<User> me() {
        return Result.success(userService.currentUser());
    }
}
