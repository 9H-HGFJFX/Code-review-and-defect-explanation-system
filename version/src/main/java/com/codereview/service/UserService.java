package com.codereview.service;

import com.codereview.vo.UserVO;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 根据用户名获取用户信息
     *
     * @param username 用户名
     * @return 用户VO（已脱敏）
     */
    UserVO getUserByUsername(String username);

    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户VO（已脱敏）
     */
    UserVO getUserById(Long userId);

    /**
     * 获取当前登录用户信息
     *
     * @param userId 用户ID
     * @return 用户VO（已脱敏）
     */
    UserVO getCurrentUser(Long userId);
}
