package com.codeaudit.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codeaudit.common.PageResult;
import com.codeaudit.common.Result;
import com.codeaudit.common.exception.BizException;
import com.codeaudit.entity.User;
import com.codeaudit.repository.UserRepository;
import com.codeaudit.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/page")
    @Operation(summary = "分页查询用户（管理员）")
    public Result<PageResult<User>> page(@RequestParam(defaultValue = "1") int current,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) String role) {
        SecurityUtil.requireRole("ADMIN");
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (role != null && !role.isBlank()) qw.eq(User::getRole, role);
        qw.orderByDesc(User::getCreateTime);
        Page<User> p = userRepository.selectPage(new Page<>(current, size), qw);
        return Result.success(PageResult.of(p.getRecords(), p.getTotal(), current, size));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "重置用户密码（管理员）")
    public Result<Void> resetPassword(@RequestParam Long userId, @RequestParam String newPassword) {
        SecurityUtil.requireRole("ADMIN");
        if (newPassword == null || newPassword.length() < 6) throw new BizException("密码至少 6 位");
        User u = userRepository.selectById(userId);
        if (u == null) throw new BizException("用户不存在");
        u.setPassword(passwordEncoder.encode(newPassword));
        userRepository.updateById(u);
        return Result.success();
    }
}
