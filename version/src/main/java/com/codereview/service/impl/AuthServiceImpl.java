package com.codereview.service.impl;

import com.codereview.common.enums.ErrorCode;
import com.codereview.common.exception.BusinessException;
import com.codereview.config.JwtTokenProvider;
import com.codereview.config.TokenBlacklistService;
import com.codereview.dto.LoginRequest;
import com.codereview.dto.LoginResponse;
import com.codereview.dto.RefreshRequest;
import com.codereview.dto.RefreshResponse;
import com.codereview.entity.User;
import com.codereview.mapper.UserMapper;
import com.codereview.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. 查询用户
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            log.warn("Login failed: user not found, username={}", request.getUsername());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "invalid credentials");
        }

        // 2. 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: invalid password, username={}", request.getUsername());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "invalid credentials");
        }

        // 3. 生成双Token
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getRole() != null ?
                        com.codereview.common.enums.UserRole.fromCode(user.getRole()) :
                        com.codereview.common.enums.UserRole.STUDENT,
                user.getClassId()
        );

        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        log.info("User logged in: userId={}, username={}", user.getId(), user.getUsername());

        // 4. 构建响应
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .user(LoginResponse.UserInfo.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .role(user.getRole() != null ?
                                com.codereview.common.enums.UserRole.fromCode(user.getRole()).name() :
                                "STUDENT")
                        .classId(user.getClassId())
                        .nickname(user.getUsername())
                        .build())
                .build();
    }

    @Override
    public RefreshResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        // 1. 验证Token有效性
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            if (jwtTokenProvider.isTokenExpired(refreshToken)) {
                throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
            }
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        // 2. 检查黑名单
        String jti = jwtTokenProvider.extractJti(refreshToken);
        if (jti != null && tokenBlacklistService.isBlacklisted(jti)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        // 3. 获取用户信息
        Long userId = jwtTokenProvider.getUserId(refreshToken);
        String username = jwtTokenProvider.getUsername(refreshToken);
        var role = jwtTokenProvider.getRole(refreshToken);
        Long classId = jwtTokenProvider.getClassId(refreshToken);

        // 4. 生成新的Access Token（不刷新Refresh Token，实现Token Rotation）
        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, username, role, classId);

        // 5. 将旧Refresh Token加入黑名单
        if (jti != null) {
            long ttl = jwtTokenProvider.getRefreshTokenTtl(refreshToken);
            tokenBlacklistService.addToBlacklist(jti, ttl);
        }

        log.info("Token refreshed: userId={}", userId);

        return RefreshResponse.builder()
                .accessToken(newAccessToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .build();
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return;
        }

        // 将Refresh Token的jti加入黑名单
        String jti = jwtTokenProvider.extractJti(refreshToken);
        if (jti != null) {
            long ttl = jwtTokenProvider.getRefreshTokenTtl(refreshToken);
            tokenBlacklistService.addToBlacklist(jti, ttl);
            log.info("User logged out: jti={}", jti);
        }
    }
}
