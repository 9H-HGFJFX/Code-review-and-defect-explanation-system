package com.codereview.controller;

import com.codereview.common.annotation.RequiresPermission;
import com.codereview.common.enums.UserRole;
import com.codereview.common.result.Result;
import com.codereview.dto.CreateRuleRequest;
import com.codereview.dto.PageRequest;
import com.codereview.dto.UpdateRuleRequest;
import com.codereview.common.result.PageResult;
import com.codereview.security.JwtUserDetails;
import com.codereview.service.RuleService;
import com.codereview.vo.RuleVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 规则控制器
 * 处理代码审查规则的CRUD操作
 */
@Slf4j
@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    /**
     * 获取规则列表
     * 权限：已登录用户
     */
    @GetMapping
    public Result<PageResult<RuleVO>> getRuleList(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int pageSize,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder) {

        PageRequest pageRequest = PageRequest.builder()
                .page(page)
                .pageSize(pageSize)
                .sortBy(sortBy)
                .sortOrder(sortOrder)
                .build();

        PageResult<RuleVO> result = ruleService.getRuleList(pageRequest, enabled, category);
        return Result.success(result);
    }

    /**
     * 创建规则
     * 权限：SUPER_ADMIN
     */
    @PostMapping
    @RequiresPermission(roles = {UserRole.SUPER_ADMIN})
    public Result<Long> createRule(@Valid @RequestBody CreateRuleRequest request,
                                   HttpServletRequest httpRequest) {
        JwtUserDetails user = getCurrentUser(httpRequest);
        Long ruleId = ruleService.createRule(request, user.getUserId());
        return Result.success(ruleId, "rule created");
    }

    /**
     * 更新规则
     * 权限：SUPER_ADMIN
     */
    @PutMapping("/{id}")
    @RequiresPermission(roles = {UserRole.SUPER_ADMIN})
    public Result<Void> updateRule(@PathVariable Long id,
                                   @Valid @RequestBody UpdateRuleRequest request,
                                   HttpServletRequest httpRequest) {
        JwtUserDetails user = getCurrentUser(httpRequest);
        ruleService.updateRule(id, request, user.getUserId());
        return Result.success("rule updated");
    }

    /**
     * 启用/禁用规则
     * 权限：SUPER_ADMIN
     */
    @PatchMapping("/{id}/enable")
    @RequiresPermission(roles = {UserRole.SUPER_ADMIN})
    public Result<Void> toggleRule(@PathVariable Long id,
                                   @RequestBody java.util.Map<String, Boolean> request,
                                   HttpServletRequest httpRequest) {
        JwtUserDetails user = getCurrentUser(httpRequest);
        ruleService.toggleRule(id, request.get("enabled"), user.getUserId());
        return Result.success("rule updated");
    }

    /**
     * 获取当前登录用户信息
     */
    private JwtUserDetails getCurrentUser(HttpServletRequest request) {
        return (JwtUserDetails) request.getAttribute("jwtUserDetails");
    }
}
