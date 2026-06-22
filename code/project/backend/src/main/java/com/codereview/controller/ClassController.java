package com.codereview.controller;

import com.codereview.common.PageRequest;
import com.codereview.common.PageResponse;
import com.codereview.common.Result;
import com.codereview.config.JwtUtil;
import com.codereview.dto.ClassMemberRequest;
import com.codereview.dto.ClassRequest;
import com.codereview.entity.ClassEntity;
import com.codereview.entity.User;
import com.codereview.exception.BusinessException;
import com.codereview.service.ClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 班级管理控制器
 */
@Tag(name = "班级管理", description = "班级CRUD、成员管理")
@RestController
@RequestMapping("/api/class")
@RequiredArgsConstructor
public class ClassController {
    
    private final ClassService classService;
    private final JwtUtil jwtUtil;
    
    @Operation(summary = "创建班级（教师）")
    @PostMapping("/add")
    public Result<ClassEntity> createClass(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ClassRequest request) {
        
        String token = authHeader.replace("Bearer ", "");
        String role = jwtUtil.getRole(token);
        
        // 只有教师可以创建班级
        if (!User.ROLE_TEACHER.equals(role)) {
            throw new BusinessException(10005, "无权限创建班级");
        }
        
        Long teacherId = jwtUtil.getUserId(token);
        ClassEntity classEntity = classService.createClass(teacherId, request);
        return Result.success(classEntity);
    }
    
    @Operation(summary = "更新班级信息")
    @PutMapping("/update/{id}")
    public Result<ClassEntity> updateClass(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @Valid @RequestBody ClassRequest request) {
        
        String token = authHeader.replace("Bearer ", "");
        Long teacherId = jwtUtil.getUserId(token);
        
        ClassEntity classEntity = classService.updateClass(id, teacherId, request);
        return Result.success(classEntity);
    }
    
    @Operation(summary = "删除班级")
    @DeleteMapping("/delete/{id}")
    public Result<?> deleteClass(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        
        String token = authHeader.replace("Bearer ", "");
        Long teacherId = jwtUtil.getUserId(token);
        
        classService.deleteClass(id, teacherId);
        return Result.success("班级已删除");
    }
    
    @Operation(summary = "分页查询班级列表")
    @GetMapping("/list")
    public Result<PageResponse<ClassEntity>> getClassList(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        
        String token = authHeader.replace("Bearer ", "");
        String role = jwtUtil.getRole(token);
        Long userId = jwtUtil.getUserId(token);
        
        PageRequest pageRequest = new PageRequest();
        pageRequest.setCurrent(current);
        pageRequest.setSize(size);
        
        // 管理员可以查看所有班级，教师只能查看自己的班级
        Long teacherId = User.ROLE_ADMIN.equals(role) ? null : userId;
        
        PageResponse<ClassEntity> result = classService.getClassList(pageRequest, teacherId);
        return Result.success(result);
    }
    
    @Operation(summary = "获取班级详情")
    @GetMapping("/{id}")
    public Result<ClassEntity> getClassDetail(@PathVariable Long id) {
        ClassEntity classEntity = classService.getClassDetail(id);
        return Result.success(classEntity);
    }
    
    @Operation(summary = "批量添加班级学生成员")
    @PostMapping("/member/add")
    public Result<?> addClassMembers(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Long classId,
            @Valid @RequestBody ClassMemberRequest request) {
        
        String token = authHeader.replace("Bearer ", "");
        Long teacherId = jwtUtil.getUserId(token);
        
        classService.addClassMembers(classId, teacherId, request);
        return Result.success("成员添加成功");
    }
    
    @Operation(summary = "批量移除班级学生成员")
    @DeleteMapping("/member/remove")
    public Result<?> removeClassMembers(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Long classId,
            @RequestBody Map<String, List<Long>> body) {
        
        String token = authHeader.replace("Bearer ", "");
        Long teacherId = jwtUtil.getUserId(token);
        List<Long> userIds = body.get("userIds");
        
        classService.removeClassMembers(classId, teacherId, userIds);
        return Result.success("成员已移除");
    }
    
    @Operation(summary = "获取班级成员列表")
    @GetMapping("/{id}/members")
    public Result<List<User>> getClassMembers(@PathVariable Long id) {
        List<User> members = classService.getClassMembers(id);
        return Result.success(members);
    }
    
    @Operation(summary = "获取用户加入的班级列表")
    @GetMapping("/my-classes")
    public Result<List<ClassEntity>> getMyClasses(
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(token);
        
        List<ClassEntity> classes = classService.getUserClasses(userId);
        return Result.success(classes);
    }
}
