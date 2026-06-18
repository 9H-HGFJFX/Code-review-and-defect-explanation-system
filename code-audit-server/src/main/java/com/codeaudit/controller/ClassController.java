package com.codeaudit.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codeaudit.common.PageResult;
import com.codeaudit.common.Result;
import com.codeaudit.common.exception.BizException;
import com.codeaudit.entity.ClassGroup;
import com.codeaudit.entity.ClassUser;
import com.codeaudit.entity.User;
import com.codeaudit.repository.ClassRepository;
import com.codeaudit.repository.ClassUserRepository;
import com.codeaudit.repository.UserRepository;
import com.codeaudit.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Tag(name = "班级管理")
@RestController
@RequestMapping("/api/class")
@RequiredArgsConstructor
public class ClassController {

    private final ClassRepository classRepository;
    private final ClassUserRepository classUserRepository;
    private final UserRepository userRepository;

    @PostMapping("/add")
    @Operation(summary = "教师创建班级")
    @Transactional
    public Result<Long> add(@RequestBody Map<String, String> body) {
        SecurityUtil.requireRole("TEACHER", "ADMIN");
        String name = body.get("name");
        if (name == null || name.isBlank()) throw new BizException("班级名称不能为空");
        ClassGroup c = new ClassGroup();
        c.setName(name);
        c.setDescription(body.get("description"));
        c.setTeacherId(SecurityUtil.currentUserId());
        c.setCreateTime(LocalDateTime.now());
        c.setUpdateTime(LocalDateTime.now());
        classRepository.insert(c);
        return Result.success(c.getId());
    }

    @GetMapping("/list")
    @Operation(summary = "当前教师名下班级")
    public Result<List<ClassGroup>> myClasses() {
        SecurityUtil.requireRole("TEACHER", "ADMIN");
        Long uid = SecurityUtil.currentUserId();
        return Result.success(classRepository.selectList(
                new LambdaQueryWrapper<ClassGroup>().eq(ClassGroup::getTeacherId, uid)));
    }

    @PostMapping("/member/add")
    @Operation(summary = "向班级批量添加学生")
    @Transactional
    public Result<Integer> addMembers(@RequestBody AddMemberReq req) {
        SecurityUtil.requireRole("TEACHER", "ADMIN");
        if (req.getClassId() == null || req.getStudentIds() == null || req.getStudentIds().isEmpty()) {
            throw new BizException("参数不完整");
        }
        int added = 0;
        for (Long sid : req.getStudentIds()) {
            // 去重
            Long exists = classUserRepository.selectCount(
                    new LambdaQueryWrapper<ClassUser>()
                            .eq(ClassUser::getClassId, req.getClassId())
                            .eq(ClassUser::getStudentId, sid));
            if (exists > 0) continue;
            ClassUser cu = new ClassUser();
            cu.setClassId(req.getClassId());
            cu.setStudentId(sid);
            cu.setJoinTime(LocalDateTime.now());
            classUserRepository.insert(cu);
            added++;
        }
        return Result.success(added);
    }

    @GetMapping("/member/list")
    @Operation(summary = "班级成员列表")
    public Result<List<User>> listMembers(@RequestParam Long classId) {
        SecurityUtil.requireRole("TEACHER", "ADMIN");
        List<ClassUser> joins = classUserRepository.selectList(
                new LambdaQueryWrapper<ClassUser>().eq(ClassUser::getClassId, classId));
        if (joins.isEmpty()) return Result.success(List.of());
        List<Long> ids = joins.stream().map(ClassUser::getStudentId).toList();
        return Result.success(userRepository.selectBatchIds(ids));
    }

    @Data
    public static class AddMemberReq {
        private Long classId;
        private List<Long> studentIds;
    }
}
