package com.codeaudit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codeaudit.common.Result;
import com.codeaudit.common.exception.BizException;
import com.codeaudit.common.exception.UnauthorizedException;
import com.codeaudit.config.JwtConfig;
import com.codeaudit.dto.LoginReq;
import com.codeaudit.dto.RegisterReq;
import com.codeaudit.entity.User;
import com.codeaudit.repository.UserRepository;
import com.codeaudit.security.JwtUtil;
import com.codeaudit.security.SecurityUtil;
import com.codeaudit.service.UserService;
import com.codeaudit.vo.LoginResp;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtConfig jwtConfig;

    @Override
    @Transactional
    public LoginResp register(RegisterReq req) {
        Long exists = userRepository.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (exists > 0) {
            throw new BizException("用户名已存在");
        }
        String role = req.getRole() == null ? "STUDENT" : req.getRole().toUpperCase();
        if (!"STUDENT".equals(role) && !"TEACHER".equals(role)) {
            role = "STUDENT"; // 注册时不允许直接成为 ADMIN
        }
        User u = new User();
        u.setUsername(req.getUsername());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setRole(role);
        u.setEmail(req.getEmail());
        u.setRealName(req.getRealName());
        userRepository.insert(u);
        log.info("[AUTH] register ok username={} role={}", u.getUsername(), u.getRole());
        return buildLoginResp(u);
    }

    @Override
    public LoginResp login(LoginReq req) {
        User u = userRepository.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (u == null || !passwordEncoder.matches(req.getPassword(), u.getPassword())) {
            throw new BizException(Result.BIZ_AUTH_FAIL, "账号或密码错误");
        }
        log.info("[AUTH] login ok username={} role={}", u.getUsername(), u.getRole());
        return buildLoginResp(u);
    }

    @Override
    public LoginResp refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("RefreshToken 不能为空");
        }
        Claims claims = jwtUtil.parse(refreshToken);
        if (claims == null || !"refresh".equals(jwtUtil.getType(claims))) {
            throw new UnauthorizedException("RefreshToken 无效或已过期");
        }
        Long uid = jwtUtil.getUserId(claims);
        User u = userRepository.selectById(uid);
        if (u == null) throw new UnauthorizedException("用户不存在");
        return buildLoginResp(u);
    }

    @Override
    public void logout(String accessToken) {
        jwtUtil.blacklist(accessToken);
    }

    @Override
    public User currentUser() {
        Long uid = SecurityUtil.currentUserId();
        User u = userRepository.selectById(uid);
        if (u == null) throw new UnauthorizedException("用户不存在");
        return u;
    }

    private LoginResp buildLoginResp(User u) {
        String access = jwtUtil.generateAccessToken(u.getId(), u.getUsername(), u.getRole());
        String refresh = jwtUtil.generateRefreshToken(u.getId(), u.getUsername(), u.getRole());
        return new LoginResp(access, refresh, jwtConfig.getAccessExpireSeconds(), LoginResp.UserInfo.from(u));
    }
}
