package com.codereview.controller;

import com.codereview.common.PageRequest;
import com.codereview.common.PageResponse;
import com.codereview.common.Result;
import com.codereview.config.JwtUtil;
import com.codereview.dto.RuleRequest;
import com.codereview.entity.Rule;
import com.codereview.entity.User;
import com.codereview.exception.BusinessException;
import com.codereview.service.AuthService;
import com.codereview.service.RuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 规则管理控制器
 */
@Tag(name = "规则管理", description = "审查规则CRUD、启用禁用、热更新")
@RestController
@RequestMapping("/api/rule")
@RequiredArgsConstructor
public class RuleController {
    
    private final RuleService ruleService;
    private final JwtUtil jwtUtil;
    
    @Operation(summary = "分页查询规则列表")
    @GetMapping("/list")
    public Result<PageResponse<Rule>> getRuleList(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        
        PageRequest pageRequest = new PageRequest();
        pageRequest.setCurrent(current);
        pageRequest.setSize(size);
        
        PageResponse<Rule> result = ruleService.getRuleList(pageRequest, category, enabled);
        return Result.success(result);
    }
    
    @Operation(summary = "获取规则详情")
    @GetMapping("/{id}")
    public Result<Rule> getRuleDetail(@PathVariable Long id) {
        Rule rule = ruleService.getById(id);
        if (rule == null) {
            return Result.fail(30001, "规则不存在");
        }
        return Result.success(rule);
    }
    
    @Operation(summary = "新增规则（教师/管理员）")
    @PostMapping("/add")
    public Result<Rule> createRule(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody RuleRequest request) {
        
        String token = authHeader.replace("Bearer ", "");
        String role = jwtUtil.getRole(token);
        
        // 只有教师和管理员可以创建规则
        if (!User.ROLE_TEACHER.equals(role) && !User.ROLE_ADMIN.equals(role)) {
            throw new BusinessException(10005, "无权限创建规则");
        }
        
        Rule rule = ruleService.createRule(request);
        return Result.success(rule);
    }
    
    @Operation(summary = "更新规则（教师/管理员）")
    @PutMapping("/update/{id}")
    public Result<Rule> updateRule(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @Valid @RequestBody RuleRequest request) {
        
        String token = authHeader.replace("Bearer ", "");
        String role = jwtUtil.getRole(token);
        
        // 只有教师和管理员可以更新规则
        if (!User.ROLE_TEACHER.equals(role) && !User.ROLE_ADMIN.equals(role)) {
            throw new BusinessException(10005, "无权限更新规则");
        }
        
        Rule rule = ruleService.updateRule(id, request);
        return Result.success(rule);
    }
    
    @Operation(summary = "启用/禁用规则")
    @PutMapping("/toggle/{id}")
    public Result<?> toggleRule(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        
        String token = authHeader.replace("Bearer ", "");
        String role = jwtUtil.getRole(token);
        
        // 只有教师和管理员可以操作
        if (!User.ROLE_TEACHER.equals(role) && !User.ROLE_ADMIN.equals(role)) {
            throw new BusinessException(10005, "无权限操作规则");
        }
        
        ruleService.toggleRule(id, enabled);
        return Result.success("规则状态已更新");
    }
    
    @Operation(summary = "删除规则")
    @DeleteMapping("/delete/{id}")
    public Result<?> deleteRule(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        
        String token = authHeader.replace("Bearer ", "");
        String role = jwtUtil.getRole(token);
        
        // 只有管理员可以删除规则
        if (!User.ROLE_ADMIN.equals(role)) {
            throw new BusinessException(10005, "无权限删除规则");
        }
        
        ruleService.deleteRule(id);
        return Result.success("规则已删除");
    }
    
    @Operation(summary = "手动刷新规则缓存（热更新）")
    @PostMapping("/refresh-cache")
    public Result<?> refreshRuleCache() {
        ruleService.refreshRuleCache();
        return Result.success("规则缓存已刷新");
    }
    
    @Operation(summary = "获取规则统计")
    @GetMapping("/statistics")
    public Result<?> getRuleStatistics() {
        Object stats = ruleService.getRuleStatistics();
        return Result.success(stats);
    }
}
