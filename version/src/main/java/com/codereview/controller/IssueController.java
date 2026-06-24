package com.codereview.controller;

import com.codereview.common.annotation.RequiresPermission;
import com.codereview.common.enums.UserRole;
import com.codereview.common.result.Result;
import com.codereview.dto.AssignIssueRequest;
import com.codereview.dto.PageRequest;
import com.codereview.dto.UpdateIssueStatusRequest;
import com.codereview.common.result.PageResult;
import com.codereview.security.JwtUserDetails;
import com.codereview.service.IssueService;
import com.codereview.vo.IssueVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 缺陷控制器
 * 处理代码缺陷的查询、分配、状态更新等操作
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;

    /**
     * 获取任务下的缺陷列表
     * 权限：任务参与者（已登录用户，自动检查班级权限）
     */
    @GetMapping("/api/tasks/{taskId}/issues")
    public Result<PageResult<IssueVO>> getIssueList(
            @PathVariable Long taskId,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int pageSize,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder) {

        PageRequest pageRequest = PageRequest.builder()
                .page(page)
                .pageSize(pageSize)
                .sortBy(sortBy)
                .sortOrder(sortOrder)
                .build();

        PageResult<IssueVO> result = issueService.getIssueListByTask(
                taskId, pageRequest, severity, status, assigneeId);
        return Result.success(result);
    }

    /**
     * 分配缺陷负责人
     * 权限：TEACHER（仅教师可以分配缺陷）
     */
    @PostMapping("/api/issues/{id}/assign")
    @RequiresPermission(roles = {UserRole.TEACHER})
    public Result<Void> assignIssue(@PathVariable Long id,
                                     @Valid @RequestBody AssignIssueRequest request,
                                     HttpServletRequest httpRequest) {
        JwtUserDetails user = getCurrentUser(httpRequest);
        issueService.assignIssue(id, request.getAssigneeId(), user.getUserId());
        return Result.success("issue assigned");
    }

    /**
     * 更新缺陷状态
     * 权限：TEACHER（任意缺陷）或 STUDENT（仅自己负责的缺陷）
     * 权限检查在Service层实现
     */
    @PatchMapping("/api/issues/{id}/status")
    @RequiresPermission(roles = {UserRole.TEACHER, UserRole.STUDENT})
    public Result<Void> updateIssueStatus(@PathVariable Long id,
                                          @Valid @RequestBody UpdateIssueStatusRequest request,
                                          HttpServletRequest httpRequest) {
        JwtUserDetails user = getCurrentUser(httpRequest);
        issueService.updateIssueStatus(id, request.getStatus(), user.getUserId());
        return Result.success("issue status updated");
    }

    /**
     * 获取当前登录用户信息
     */
    private JwtUserDetails getCurrentUser(HttpServletRequest request) {
        return (JwtUserDetails) request.getAttribute("jwtUserDetails");
    }
}
