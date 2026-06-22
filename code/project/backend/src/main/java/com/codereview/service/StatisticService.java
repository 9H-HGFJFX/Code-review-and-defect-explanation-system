package com.codereview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codereview.common.PageRequest;
import com.codereview.common.PageResponse;
import com.codereview.entity.Issue;
import com.codereview.entity.Review;
import com.codereview.entity.User;
import com.codereview.mapper.IssueMapper;
import com.codereview.mapper.ReviewMapper;
import com.codereview.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计分析服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticService {
    
    private final ReviewMapper reviewMapper;
    private final IssueMapper issueMapper;
    private final UserMapper userMapper;
    
    /**
     * 获取全局概览统计
     */
    public Map<String, Object> getOverviewStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 总审查次数
        long totalReviews = reviewMapper.selectCount(null);
        stats.put("totalReviews", totalReviews);
        
        // 总用户数
        long totalUsers = userMapper.selectCount(null);
        stats.put("totalUsers", totalUsers);
        
        // 总问题数
        long totalIssues = issueMapper.selectCount(null);
        stats.put("totalIssues", totalIssues);
        
        // 今日审查次数
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayReviews = reviewMapper.selectCount(
            new LambdaQueryWrapper<Review>().ge(Review::getReviewTime, todayStart)
        );
        stats.put("todayReviews", todayReviews);
        
        // 问题分布
        stats.put("issueDistribution", getIssueDistribution(null));
        
        return stats;
    }
    
    /**
     * 获取用户概览统计
     */
    public Map<String, Object> getUserOverviewStatistics(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 用户的总审查次数
        long totalReviews = reviewMapper.selectCount(
            new LambdaQueryWrapper<Review>().eq(Review::getUserId, userId)
        );
        stats.put("totalReviews", totalReviews);
        
        // 用户的总问题数
        List<Long> reviewIds = reviewMapper.selectList(
            new LambdaQueryWrapper<Review>().eq(Review::getUserId, userId)
        ).stream().map(Review::getId).collect(Collectors.toList());
        
        long totalIssues = 0;
        if (!reviewIds.isEmpty()) {
            totalIssues = issueMapper.selectCount(
                new LambdaQueryWrapper<Issue>().in(Issue::getReviewId, reviewIds)
            );
        }
        stats.put("totalIssues", totalIssues);
        
        // 今日审查次数
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayReviews = reviewMapper.selectCount(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .ge(Review::getReviewTime, todayStart)
        );
        stats.put("todayReviews", todayReviews);
        
        // 问题分布
        stats.put("issueDistribution", getIssueDistribution(userId));
        
        return stats;
    }
    
    /**
     * 获取班级概览统计
     */
    public Map<String, Object> getClassOverviewStatistics(Long classId, List<Long> studentIds) {
        Map<String, Object> stats = new HashMap<>();
        
        if (studentIds == null || studentIds.isEmpty()) {
            stats.put("totalReviews", 0);
            stats.put("totalIssues", 0);
            stats.put("todayReviews", 0);
            stats.put("issueDistribution", new HashMap<>());
            return stats;
        }
        
        // 班级总审查次数
        long totalReviews = reviewMapper.selectCount(
            new LambdaQueryWrapper<Review>().in(Review::getUserId, studentIds)
        );
        stats.put("totalReviews", totalReviews);
        
        // 班级总问题数
        List<Long> reviewIds = reviewMapper.selectList(
            new LambdaQueryWrapper<Review>().in(Review::getUserId, studentIds)
        ).stream().map(Review::getId).collect(Collectors.toList());
        
        long totalIssues = 0;
        if (!reviewIds.isEmpty()) {
            totalIssues = issueMapper.selectCount(
                new LambdaQueryWrapper<Issue>().in(Issue::getReviewId, reviewIds)
            );
        }
        stats.put("totalIssues", totalIssues);
        
        // 今日审查次数
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayReviews = reviewMapper.selectCount(
            new LambdaQueryWrapper<Review>()
                .in(Review::getUserId, studentIds)
                .ge(Review::getReviewTime, todayStart)
        );
        stats.put("todayReviews", todayReviews);
        
        // 问题分布
        stats.put("issueDistribution", getIssueDistributionByReviews(reviewIds));
        
        return stats;
    }
    
    /**
     * 获取问题分布统计
     */
    public Map<String, Long> getIssueDistribution(Long userId) {
        List<Long> reviewIds;
        
        if (userId != null) {
            reviewIds = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>().eq(Review::getUserId, userId)
            ).stream().map(Review::getId).collect(Collectors.toList());
        } else {
            reviewIds = reviewMapper.selectList(null).stream().map(Review::getId).collect(Collectors.toList());
        }
        
        return getIssueDistributionByReviews(reviewIds);
    }
    
    /**
     * 根据审查记录ID列表获取问题分布
     */
    private Map<String, Long> getIssueDistributionByReviews(List<Long> reviewIds) {
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("CRITICAL", 0L);
        distribution.put("ERROR", 0L);
        distribution.put("WARNING", 0L);
        distribution.put("SUGGESTION", 0L);
        
        if (reviewIds == null || reviewIds.isEmpty()) {
            return distribution;
        }
        
        List<Issue> issues = issueMapper.selectList(
            new LambdaQueryWrapper<Issue>().in(Issue::getReviewId, reviewIds)
        );
        
        for (Issue issue : issues) {
            String severity = issue.getSeverity();
            distribution.merge(severity, 1L, Long::sum);
        }
        
        return distribution;
    }
    
    /**
     * 获取审查趋势（最近N天）
     */
    public List<Map<String, Object>> getReviewTrend(int days) {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
            
            long count = reviewMapper.selectCount(
                new LambdaQueryWrapper<Review>()
                    .ge(Review::getReviewTime, dayStart)
                    .le(Review::getReviewTime, dayEnd)
            );
            
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toString());
            dayData.put("count", count);
            trend.add(dayData);
        }
        
        return trend;
    }
    
    /**
     * 获取问题趋势（最近N天）
     */
    public List<Map<String, Object>> getIssueTrend(int days) {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
            
            // 获取当天的审查记录
            List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                    .ge(Review::getReviewTime, dayStart)
                    .le(Review::getReviewTime, dayEnd)
            );
            
            List<Long> reviewIds = reviews.stream().map(Review::getId).collect(Collectors.toList());
            
            long critical = 0, error = 0, warning = 0, suggestion = 0;
            
            if (!reviewIds.isEmpty()) {
                List<Issue> issues = issueMapper.selectList(
                    new LambdaQueryWrapper<Issue>().in(Issue::getReviewId, reviewIds)
                );
                
                for (Issue issue : issues) {
                    switch (issue.getSeverity()) {
                        case Issue.SEVERITY_CRITICAL -> critical++;
                        case Issue.SEVERITY_ERROR -> error++;
                        case Issue.SEVERITY_WARNING -> warning++;
                        case Issue.SEVERITY_SUGGESTION -> suggestion++;
                    }
                }
            }
            
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toString());
            dayData.put("critical", critical);
            dayData.put("error", error);
            dayData.put("warning", warning);
            dayData.put("suggestion", suggestion);
            trend.add(dayData);
        }
        
        return trend;
    }
    
    /**
     * 获取问题详细分布（按规则分类）
     */
    public Map<String, Long> getIssueDistributionByCategory(Long userId) {
        Map<String, Long> distribution = new LinkedHashMap<>();
        
        List<Long> reviewIds;
        if (userId != null) {
            reviewIds = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>().eq(Review::getUserId, userId)
            ).stream().map(Review::getId).collect(Collectors.toList());
        } else {
            reviewIds = reviewMapper.selectList(null).stream().map(Review::getId).collect(Collectors.toList());
        }
        
        if (reviewIds.isEmpty()) {
            distribution.put("SECURITY", 0L);
            distribution.put("DEFECT", 0L);
            distribution.put("STYLE", 0L);
            return distribution;
        }
        
        List<Issue> issues = issueMapper.selectList(
            new LambdaQueryWrapper<Issue>().in(Issue::getReviewId, reviewIds)
        );
        
        // 简化实现：按严重级别分组
        distribution.put("SECURITY", issues.stream().filter(i -> Issue.SEVERITY_CRITICAL.equals(i.getSeverity())).count());
        distribution.put("DEFECT", issues.stream().filter(i -> Issue.SEVERITY_ERROR.equals(i.getSeverity())).count());
        distribution.put("STYLE", issues.stream().filter(i -> Issue.SEVERITY_WARNING.equals(i.getSeverity())).count());
        
        return distribution;
    }
}
