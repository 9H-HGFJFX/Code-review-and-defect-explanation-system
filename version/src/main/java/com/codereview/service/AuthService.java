package com.codereview.service;

import com.codereview.dto.LoginRequest;
import com.codereview.dto.LoginResponse;
import com.codereview.dto.RefreshRequest;
import com.codereview.dto.RefreshResponse;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应（包含双Token）
     */
    LoginResponse login(LoginRequest request);

    /**
     * 刷新Token
     *
     * @param request 刷新请求
     * @return 新的Access Token
     */
    RefreshResponse refresh(RefreshRequest request);

    /**
     * 用户登出
     *
     * @param refreshToken Refresh Token
     */
    void logout(String refreshToken);
}
