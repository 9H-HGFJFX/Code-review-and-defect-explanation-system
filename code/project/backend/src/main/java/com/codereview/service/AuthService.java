package com.codereview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codereview.common.Result;
import com.codereview.config.JwtProperties;
import com.codereview.config.JwtUtil;
import com.codereview.dto.LoginRequest;
import com.codereview.dto.LoginResponse;
import com.codereview.dto.RegisterRequest;
import com.codereview.entity.User;
import com.codereview.exception.BusinessException;
import com.codereview.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService extends ServiceImpl<UserMapper, User> {
    
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder;
    
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;
    
    /**
     * 用户注册
     */
    public Result<?> register(RegisterRequest request) {
        // 检查用户名是否已存在
        if (lambdaQuery().eq(User::getUsername, request.getUsername()).exists()) {
            return Result.fail("用户名已存在");
        }
        
        // 验证角色
        String role = request.getRole();
        if (role == null || (!role.equals(User.ROLE_STUDENT) && !role.equals(User.ROLE_TEACHER))) {
            role = User.ROLE_STUDENT; // 默认学生角色
        }
        
        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setEmail(request.getEmail());
        
        if (!save(user)) {
            return Result.fail("注册失败");
        }
        
        log.info("用户注册成功: {}", request.getUsername());
        return Result.success("注册成功");
    }
    
    /**
     * 用户登录
     */
    public Result<LoginResponse> login(LoginRequest request) {
        String username = request.getUsername();
        
        // 检查账号是否被锁定
        String lockKey = "account:lock:" + username;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            return Result.fail(Result.ERR_ACCOUNT_LOCKED, "账号已被锁定，请" + LOCK_DURATION_MINUTES + "分钟后重试");
        }
        
        // 查询用户
        User user = lambdaQuery().eq(User::getUsername, username).one();
        if (user == null) {
            recordLoginFailed(lockKey);
            return Result.fail(Result.ERR_ACCOUNT_PASSWORD, "账号或密码错误");
        }
        
        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordLoginFailed(lockKey);
            return Result.fail(Result.ERR_ACCOUNT_PASSWORD, "账号或密码错误");
        }
        
        // 登录成功，生成Token
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());
        
        // 清除登录失败记录
        redisTemplate.delete(lockKey);
        redisTemplate.delete("login:attempts:" + username);
        
        log.info("用户登录成功: {}", username);
        
        LoginResponse response = LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpiration() / 1000)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
        
        return Result.success(response);
    }
    
    /**
     * 刷新Token
     */
    public Result<LoginResponse> refreshToken(String refreshToken) {
        try {
            // 验证Token
            if (!jwtUtil.validateToken(refreshToken)) {
                return Result.fail(Result.ERR_TOKEN_INVALID, "无效的Refresh Token");
            }
            
            // 检查是否为Refresh Token
            if (!"refresh".equals(jwtUtil.getTokenType(refreshToken))) {
                return Result.fail(Result.ERR_TOKEN_INVALID, "无效的Token类型");
            }
            
            // 检查Token是否在黑名单
            if (isTokenBlacklisted(refreshToken)) {
                return Result.fail(Result.ERR_TOKEN_INVALID, "Token已失效");
            }
            
            // 获取用户信息
            Long userId = jwtUtil.getUserId(refreshToken);
            User user = getById(userId);
            if (user == null) {
                return Result.fail(Result.ERR_TOKEN_INVALID, "用户不存在");
            }
            
            // 生成新的Access Token
            String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
            
            // 将旧的Refresh Token加入黑名单
            addToBlacklist(refreshToken);
            
            LoginResponse response = LoginResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken) // 复用旧的refresh token
                    .tokenType("Bearer")
                    .expiresIn(jwtProperties.getAccessTokenExpiration() / 1000)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .role(user.getRole())
                    .build();
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("刷新Token失败", e);
            return Result.fail(Result.ERR_TOKEN_INVALID, "Token已过期，请重新登录");
        }
    }
    
    /**
     * 用户登出
     */
    public Result<?> logout(String token) {
        try {
            // 将Token加入黑名单
            addToBlacklist(token);
            
            // 获取用户名并清除相关缓存
            String username = jwtUtil.getUsername(token);
            redisTemplate.delete("login:attempts:" + username);
            
            log.info("用户登出: {}", username);
            return Result.success("登出成功");
        } catch (Exception e) {
            log.error("登出失败", e);
            return Result.success("登出成功");
        }
    }
    
    /**
     * 获取当前用户信息
     */
    public User getCurrentUser(String token) {
        Long userId = jwtUtil.getUserId(token);
        return getById(userId);
    }
    
    /**
     * 记录登录失败
     */
    private void recordLoginFailed(String lockKey) {
        String attemptsKey = "login:attempts:" + (lockKey.replace("account:lock:", ""));
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptsKey, LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
        }
        
        if (attempts != null && attempts >= MAX_LOGIN_ATTEMPTS) {
            redisTemplate.opsForValue().set(lockKey, "locked", LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
            log.warn("账号{}因连续登录失败被锁定{}分钟", lockKey.replace("account:lock:", ""), LOCK_DURATION_MINUTES);
        }
    }
    
    /**
     * 将Token加入黑名单
     */
    private void addToBlacklist(String token) {
        try {
            Claims claims = jwtUtil.parseToken(token);
            Date expiration = claims.getExpiration();
            long ttl = (expiration.getTime() - System.currentTimeMillis()) / 1000;
            
            if (ttl > 0) {
                String blacklistKey = BLACKLIST_PREFIX + token;
                redisTemplate.opsForValue().set(blacklistKey, "1", ttl, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.warn("Token黑名单设置失败", e);
        }
    }
    
    /**
     * 检查Token是否在黑名单
     */
    private boolean isTokenBlacklisted(String token) {
        String blacklistKey = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
    }
}
