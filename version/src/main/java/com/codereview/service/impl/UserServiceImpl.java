package com.codereview.service.impl;

import com.codereview.common.enums.ErrorCode;
import com.codereview.common.enums.UserRole;
import com.codereview.common.exception.ResourceNotFoundException;
import com.codereview.entity.User;
import com.codereview.mapper.UserMapper;
import com.codereview.service.UserService;
import com.codereview.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public UserVO getUserByUsername(String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            return null;
        }
        return convertToVO(user);
    }

    @Override
    public UserVO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return convertToVO(user);
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User", userId);
        }
        return convertToVO(user);
    }

    /**
     * 实体转换为VO（带数据脱敏）
     */
    private UserVO convertToVO(User user) {
        return UserVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                // 邮箱脱敏：138****5678@***.com 格式
                .email(maskEmail(user.getEmail()))
                .role(user.getRole() != null ?
                        UserRole.fromCode(user.getRole()).name() : "STUDENT")
                .classId(user.getClassId())
                .build();
    }

    /**
     * 邮箱脱敏
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return email;
        }
        String username = parts[0];
        String domain = parts[1];

        if (username.length() <= 2) {
            username = username + "***";
        } else {
            username = username.substring(0, username.length() - 2) + "***";
        }

        return username + "@***." + (domain.contains(".") ?
                domain.substring(domain.lastIndexOf(".") + 1) : "com");
    }
}
