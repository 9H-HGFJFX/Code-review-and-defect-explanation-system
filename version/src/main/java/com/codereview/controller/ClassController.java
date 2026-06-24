package com.codereview.controller;

import com.codereview.common.annotation.RequiresPermission;
import com.codereview.common.enums.UserRole;
import com.codereview.common.result.Result;
import com.codereview.dto.AddStudentRequest;
import com.codereview.dto.CreateClassRequest;
import com.codereview.security.JwtUserDetails;
import com.codereview.service.ClassService;
import com.codereview.vo.ClassVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 班级控制器
 * 处理班级的CRUD和学生管理操作
 */
@Slf4j
@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    /**
     * 创建班级
     * 权限：SUPER_ADMIN
     */
    @PostMapping
    @RequiresPermission(roles = {UserRole.SUPER_ADMIN})
    public Result<Long> createClass(@Valid @RequestBody CreateClassRequest request,
                                    HttpServletRequest httpRequest) {
        JwtUserDetails user = getCurrentUser(httpRequest);
        Long classId = classService.createClass(request, user.getUserId());
        return Result.success(classId, "class created");
    }

    /**
     * 获取班级列表
     * 权限：已登录用户（自动带classId过滤）
     */
    @GetMapping
    public Result<List<ClassVO>> getClassList(@RequestParam(required = false) Long classId) {
        List<ClassVO> result = classService.getClassList(classId);
        return Result.success(result);
    }

    /**
     * 获取班级详情
     * 权限：已登录用户（自动检查班级权限）
     */
    @GetMapping("/{id}")
    public Result<ClassVO> getClassDetail(@PathVariable Long id) {
        ClassVO detail = classService.getClassDetail(id);
        return Result.success(detail);
    }

    /**
     * 更新班级
     * 权限：SUPER_ADMIN
     */
    @PutMapping("/{id}")
    @RequiresPermission(roles = {UserRole.SUPER_ADMIN})
    public Result<Void> updateClass(@PathVariable Long id,
                                    @Valid @RequestBody CreateClassRequest request,
                                    HttpServletRequest httpRequest) {
        JwtUserDetails user = getCurrentUser(httpRequest);
        classService.updateClass(id, request, user.getUserId());
        return Result.success("class updated");
    }

    /**
     * 删除班级
     * 权限：SUPER_ADMIN
     */
    @DeleteMapping("/{id}")
    @RequiresPermission(roles = {UserRole.SUPER_ADMIN})
    public Result<Void> deleteClass(@PathVariable Long id,
                                    HttpServletRequest httpRequest) {
        JwtUserDetails user = getCurrentUser(httpRequest);
        classService.deleteClass(id, user.getUserId());
        return Result.success("class deleted");
    }

    /**
     * 添加学生到班级
     * 权限：TEACHER
     */
    @PostMapping("/{id}/students")
    @RequiresPermission(roles = {UserRole.TEACHER, UserRole.SUPER_ADMIN})
    public Result<Void> addStudent(@PathVariable Long id,
                                    @Valid @RequestBody AddStudentRequest request,
                                    HttpServletRequest httpRequest) {
        JwtUserDetails user = getCurrentUser(httpRequest);
        classService.addStudent(id, request.getStudentId(), user.getUserId());
        return Result.success("student added");
    }

    /**
     * 获取班级学生列表
     * 权限：TEACHER, SUPER_ADMIN
     */
    @GetMapping("/{id}/students")
    public Result<List<ClassVO>> getClassStudents(@PathVariable Long id) {
        List<ClassVO> students = classService.getClassStudents(id);
        return Result.success(students);
    }

    /**
     * 获取当前登录用户信息
     */
    private JwtUserDetails getCurrentUser(HttpServletRequest request) {
        return (JwtUserDetails) request.getAttribute("jwtUserDetails");
    }
}
