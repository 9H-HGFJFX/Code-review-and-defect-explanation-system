package com.codereview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codereview.common.PageRequest;
import com.codereview.common.PageResponse;
import com.codereview.dto.ReviewResultDTO;
import com.codereview.dto.ReviewSubmitRequest;
import com.codereview.engine.ReviewEngine;
import com.codereview.engine.ReviewResponse;
import com.codereview.entity.Issue;
import com.codereview.entity.Review;
import com.codereview.entity.Rule;
import com.codereview.exception.BusinessException;
import com.codereview.exception.CodeLengthExceededException;
import com.codereview.exception.CodeSyntaxException;
import com.codereview.mapper.IssueMapper;
import com.codereview.mapper.ReviewMapper;
import com.codereview.mapper.RuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 代码审查服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService extends ServiceImpl<ReviewMapper, Review> {
    
    private final ReviewEngine reviewEngine;
    private final RuleService ruleService;
    private final IssueMapper issueMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${review.engine.max-lines:5000}")
    private int maxLines;
    
    @Value("${review.engine.sync-threshold:500}")
    private int syncThreshold;
    
    @Value("${review.engine.timeout-seconds:30}")
    private int timeoutSeconds;
    
    /**
     * 提交代码审查
     */
    @Transactional
    public ReviewResultDTO submitReview(Long userId, String username, ReviewSubmitRequest request) {
        String code = request.getCodeContent();
        
        // 1. 行数校验
        int lineCount = (int) code.lines().count();
        if (lineCount > maxLines) {
            throw new CodeLengthExceededException(lineCount, maxLines);
        }
        
        if (lineCount == 0) {
            throw new BusinessException(400, "代码不能为空");
        }
        
        // 2. 判断同步/异步处理
        boolean isAsync = lineCount > syncThreshold;
        
        // 3. 创建审查记录
        Review review = new Review();
        review.setUserId(userId);
        review.setCodeContent(code);
        review.setFileName(request.getFileName());
        review.setLineCount(lineCount);
        review.setReviewTime(LocalDateTime.now());
        
        if (isAsync) {
            review.setStatus(Review.STATUS_PENDING);
            review.setTaskId(UUID.randomUUID().toString());
        } else {
            review.setStatus(Review.STATUS_PENDING);
        }
        
        save(review);
        
        // 4. 执行审查
        if (isAsync) {
            // 异步处理
            executeAsyncReview(review.getId(), request);
            return buildResultDTO(review, username, List.of(), isAsync);
        } else {
            // 同步处理
            ReviewResponse response = reviewEngine.review(code, getApplicableRules(request.getClassId()), timeoutSeconds);
            
            if (response.isSuccess()) {
                // 保存问题
                saveIssues(response.getIssues(), review.getId());
                review.setStatus(Review.STATUS_COMPLETED);
            } else {
                review.setStatus(Review.STATUS_FAILED);
            }
            updateById(review);
            
            return buildResultDTO(review, username, response.getIssues(), isAsync);
        }
    }
    
    /**
     * 异步执行审查
     */
    @Async("reviewExecutor")
    public void executeAsyncReview(Long reviewId, ReviewSubmitRequest request) {
        try {
            Review review = getById(reviewId);
            if (review == null) {
                log.error("审查记录不存在: {}", reviewId);
                return;
            }
            
            // 执行审查
            ReviewResponse response = reviewEngine.review(
                request.getCodeContent(), 
                getApplicableRules(request.getClassId()), 
                timeoutSeconds
            );
            
            // 更新结果
            if (response.isSuccess()) {
                saveIssues(response.getIssues(), reviewId);
                review.setStatus(Review.STATUS_COMPLETED);
            } else {
                review.setStatus(Review.STATUS_FAILED);
            }
            updateById(review);
            
            // 存储结果供前端轮询
            String resultKey = "review:result:" + review.getTaskId();
            redisTemplate.opsForValue().set(resultKey, buildResultDTO(review, null, response.getIssues(), true));
            redisTemplate.expire(resultKey, 1, TimeUnit.HOURS);
            
            log.info("异步审查完成: reviewId={}, issues={}", reviewId, response.getIssues().size());
            
        } catch (Exception e) {
            log.error("异步审查异常: reviewId={}", reviewId, e);
            Review review = getById(reviewId);
            if (review != null) {
                review.setStatus(Review.STATUS_FAILED);
                updateById(review);
            }
        }
    }
    
    /**
     * 获取审查结果
     */
    public ReviewResultDTO getReviewResult(Long reviewId, Long userId) {
        Review review = lambdaQuery()
                .eq(Review::getId, reviewId)
                .one();
        
        if (review == null) {
            throw new BusinessException(20004, "审查记录不存在");
        }
        
        // 检查权限（学生只能查看自己的记录）
        if (!review.getUserId().equals(userId)) {
            throw new BusinessException(10005, "无权限查看此审查记录");
        }
        
        // 查询问题列表
        List<Issue> issues = issueMapper.selectList(
            new LambdaQueryWrapper<Issue>().eq(Issue::getReviewId, reviewId)
        );
        
        String username = null; // 可以在查询中联表获取
        return buildResultDTO(review, username, issues, false);
    }
    
    /**
     * 轮询异步审查结果
     */
    public ReviewResultDTO pollAsyncResult(String taskId, Long userId) {
        String resultKey = "review:result:" + taskId;
        Object cached = redisTemplate.opsForValue().get(resultKey);
        
        if (cached != null) {
            return (ReviewResultDTO) cached;
        }
        
        // 从数据库查询
        Review review = lambdaQuery()
                .eq(Review::getTaskId, taskId)
                .one();
        
        if (review == null) {
            throw new BusinessException(20004, "任务不存在");
        }
        
        // 检查用户权限
        if (!review.getUserId().equals(userId)) {
            throw new BusinessException(10005, "无权限查看此审查记录");
        }
        
        List<Issue> issues = issueMapper.selectList(
            new LambdaQueryWrapper<Issue>().eq(Issue::getReviewId, review.getId())
        );
        
        return buildResultDTO(review, null, issues, true);
    }
    
    /**
     * 分页查询审查记录
     */
    public PageResponse<ReviewResultDTO> getReviewList(Long userId, String role, PageRequest pageRequest) {
        Page<Review> page = new Page<>(pageRequest.getCurrent(), pageRequest.getSize());
        
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getUserId, userId);
        wrapper.orderByDesc(Review::getReviewTime);
        
        Page<Review> resultPage = page(page, wrapper);
        
        List<ReviewResultDTO> records = resultPage.getRecords().stream()
                .map(r -> buildResultDTO(r, null, List.of(), false))
                .collect(Collectors.toList());
        
        return PageResponse.ok(
            records, 
            resultPage.getTotal(), 
            resultPage.getCurrent(), 
            resultPage.getSize()
        );
    }
    
    /**
     * 保存问题列表
     */
    private void saveIssues(List<Issue> issues, Long reviewId) {
        for (Issue issue : issues) {
            issue.setReviewId(reviewId);
            issueMapper.insert(issue);
        }
    }
    
    /**
     * 获取适用的规则
     */
    private List<Rule> getApplicableRules(Long classId) {
        return ruleService.getEnabledRules(classId);
    }
    
    /**
     * 构建结果DTO
     */
    private ReviewResultDTO buildResultDTO(Review review, String username, List<Issue> issues, boolean isAsync) {
        int critical = 0, error = 0, warning = 0, suggestion = 0;
        
        for (Issue issue : issues) {
            switch (issue.getSeverity()) {
                case Issue.SEVERITY_CRITICAL -> critical++;
                case Issue.SEVERITY_ERROR -> error++;
                case Issue.SEVERITY_WARNING -> warning++;
                case Issue.SEVERITY_SUGGESTION -> suggestion++;
            }
        }
        
        return ReviewResultDTO.builder()
                .id(review.getId())
                .userId(review.getUserId())
                .username(username)
                .fileName(review.getFileName())
                .lineCount(review.getLineCount())
                .reviewTime(review.getReviewTime())
                .status(review.getStatus())
                .taskId(isAsync ? review.getTaskId() : null)
                .issues(issues)
                .totalIssues(issues.size())
                .criticalCount(critical)
                .errorCount(error)
                .warningCount(warning)
                .suggestionCount(suggestion)
                .build();
    }
}
