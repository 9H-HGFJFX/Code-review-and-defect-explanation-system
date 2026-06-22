package com.codereview.controller;

import com.codereview.common.Result;
import com.codereview.config.JwtUtil;
import com.codereview.entity.ClassEntity;
import com.codereview.entity.ClassUser;
import com.codereview.entity.User;
import com.codereview.mapper.ClassUserMapper;
import com.codereview.service.ClassService;
import com.codereview.service.StatisticService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统计分析控制器
 */
@Tag(name = "数据统计", description = "审查统计、问题分布、趋势分析")
@RestController
@RequestMapping("/api/statistic")
@RequiredArgsConstructor
public class StatisticController {
    
    private final StatisticService statisticService;
    private final ClassService classService;
    private final JwtUtil jwtUtil;
    private final ClassUserMapper classUserMapper;
    
    @Operation(summary = "获取全局统计概览")
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverviewStatistics() {
        Map<String, Object> stats = statisticService.getOverviewStatistics();
        return Result.success(stats);
    }
    
    @Operation(summary = "获取用户统计概览")
    @GetMapping("/user/overview")
    public Result<Map<String, Object>> getUserOverviewStatistics(
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(token);
        
        Map<String, Object> stats = statisticService.getUserOverviewStatistics(userId);
        return Result.success(stats);
    }
    
    @Operation(summary = "获取班级统计概览")
    @GetMapping("/class/overview")
    public Result<Map<String, Object>> getClassOverviewStatistics(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Long classId) {
        
        // 获取班级成员
        List<User> members = classService.getClassMembers(classId);
        List<Long> studentIds = members.stream().map(User::getId).collect(Collectors.toList());
        
        Map<String, Object> stats = statisticService.getClassOverviewStatistics(classId, studentIds);
        return Result.success(stats);
    }
    
    @Operation(summary = "获取审查趋势（最近N天）")
    @GetMapping("/review/trend")
    public Result<List<Map<String, Object>>> getReviewTrend(
            @RequestParam(defaultValue = "7") int days) {
        
        List<Map<String, Object>> trend = statisticService.getReviewTrend(days);
        return Result.success(trend);
    }
    
    @Operation(summary = "获取问题趋势（最近N天）")
    @GetMapping("/issue/trend")
    public Result<List<Map<String, Object>>> getIssueTrend(
            @RequestParam(defaultValue = "7") int days) {
        
        List<Map<String, Object>> trend = statisticService.getIssueTrend(days);
        return Result.success(trend);
    }
    
    @Operation(summary = "获取问题分布（按严重级别）")
    @GetMapping("/issue/distribution")
    public Result<Map<String, Long>> getIssueDistribution(
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.replace("Bearer ", "");
        String role = jwtUtil.getRole(token);
        
        Map<String, Long> distribution;
        if (User.ROLE_ADMIN.equals(role)) {
            distribution = statisticService.getIssueDistribution(null);
        } else {
            Long userId = jwtUtil.getUserId(token);
            distribution = statisticService.getIssueDistribution(userId);
        }
        
        return Result.success(distribution);
    }
    
    @Operation(summary = "获取问题分布（按规则分类）")
    @GetMapping("/issue/category")
    public Result<Map<String, Long>> getIssueDistributionByCategory(
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.replace("Bearer ", "");
        String role = jwtUtil.getRole(token);
        
        Map<String, Long> distribution;
        if (User.ROLE_ADMIN.equals(role)) {
            distribution = statisticService.getIssueDistributionByCategory(null);
        } else {
            Long userId = jwtUtil.getUserId(token);
            distribution = statisticService.getIssueDistributionByCategory(userId);
        }
        
        return Result.success(distribution);
    }
}
