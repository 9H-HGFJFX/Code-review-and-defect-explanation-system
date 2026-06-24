package com.codereview.controller;

import com.codereview.common.annotation.RequiresPermission;
import com.codereview.common.enums.UserRole;
import com.codereview.common.result.Result;
import com.codereview.dto.CreateTaskRequest;
import com.codereview.dto.PageRequest;
import com.codereview.common.result.PageResult;
import com.codereview.security.JwtUserDetails;
import com.codereview.security.JwtUserDetailsAdapter;
import com.codereview.service.ReviewTaskService;
import com.codereview.vo.TaskDetailVO;
import com.codereview.vo.TaskVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 审查任务控制器
 * 处理审查任务的CRUD操作
 */
@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class ReviewTaskController {

    private final ReviewTaskService reviewTaskService;

    /**
     * 创建审查任务
     * 权限：TEACHER, SUPER_ADMIN
     */
    @PostMapping
    @RequiresPermission(roles = {UserRole.TEACHER, UserRole.SUPER_ADMIN})
    public Result<Long> createTask(@Valid @RequestBody CreateTaskRequest request,
                                   HttpServletRequest httpRequest) {
        JwtUserDetails user = getCurrentUser(httpRequest);
        Long taskId = reviewTaskService.createTask(request, user.getUserId());
        return Result.success(taskId, "task created");
    }

    /**
     * 获取任务列表
     * 权限：已登录用户（自动带classId过滤）
     */
    @GetMapping
    public Result<PageResult<TaskVO>> getTaskList(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder) {

        PageRequest pageRequest = PageRequest.builder()
                .page(page)
                .pageSize(pageSize)
                .sortBy(sortBy)
                .sortOrder(sortOrder)
                .build();

        PageResult<TaskVO> result = reviewTaskService.getTaskList(pageRequest, classId, status, keyword);
        return Result.success(result);
    }

    /**
     * 获取任务详情
     * 权限：任务参与者（已登录用户，自动检查班级权限）
     */
    @GetMapping("/{id}")
    public Result<TaskDetailVO> getTaskDetail(@PathVariable Long id) {
        TaskDetailVO detail = reviewTaskService.getTaskDetail(id);
        return Result.success(detail);
    }

    /**
     * 更新任务状态
     * 权限：TEACHER（任务创建者）
     */
    @PatchMapping("/{id}/status")
    @RequiresPermission(roles = {UserRole.TEACHER, UserRole.SUPER_ADMIN})
    public Result<Void> updateTaskStatus(@PathVariable Long id,
                                         @RequestBody java.util.Map<String, String> request,
                                         HttpServletRequest httpRequest) {
        JwtUserDetails user = getCurrentUser(httpRequest);
        reviewTaskService.updateTaskStatus(id, request.get("status"), user.getUserId());
        return Result.success("task status updated");
    }

    /**
     * 删除任务
     * 权限：SUPER_ADMIN
     */
    @DeleteMapping("/{id}")
    @RequiresPermission(roles = {UserRole.SUPER_ADMIN})
    public Result<Void> deleteTask(@PathVariable Long id) {
        reviewTaskService.deleteTask(id);
        return Result.success("task deleted");
    }

    /**
     * 获取当前登录用户信息
     */
    private JwtUserDetails getCurrentUser(HttpServletRequest request) {
        return (JwtUserDetails) request.getAttribute("jwtUserDetails");
    }
}
