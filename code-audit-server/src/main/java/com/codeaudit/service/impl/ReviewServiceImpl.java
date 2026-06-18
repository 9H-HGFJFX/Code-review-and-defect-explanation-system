package com.codeaudit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codeaudit.ai.MiniMaxClient;
import com.codeaudit.common.Result;
import com.codeaudit.common.exception.BizException;
import com.codeaudit.common.exception.CodeParseException;
import com.codeaudit.common.exception.CodeTooLargeException;
import com.codeaudit.common.exception.ForbiddenException;
import com.codeaudit.config.CodeAuditProperties;
import com.codeaudit.dto.ReviewSubmitReq;
import com.codeaudit.engine.IssueDraft;
import com.codeaudit.engine.ReviewContext;
import com.codeaudit.engine.ReviewEngine;
import com.codeaudit.entity.Issue;
import com.codeaudit.entity.Review;
import com.codeaudit.entity.Rule;
import com.codeaudit.repository.IssueRepository;
import com.codeaudit.repository.ReviewRepository;
import com.codeaudit.service.ReviewService;
import com.codeaudit.service.RuleService;
import com.codeaudit.vo.IssueVO;
import com.codeaudit.vo.ReviewResultVO;
import com.codeaudit.vo.SeverityStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewEngine reviewEngine;
    private final RuleService ruleService;
    private final ReviewRepository reviewRepository;
    private final IssueRepository issueRepository;
    private final CodeAuditProperties properties;
    private final MiniMaxClient m3Client;

    @Override
    @Transactional
    public ReviewResultVO submit(Long userId, ReviewSubmitReq req) {
        if (req.getCode() == null || req.getCode().isBlank()) {
            throw new BizException("代码内容不能为空");
        }
        // 行数校验 - 概要设计 1.2 / 需求分析 4.1
        int lineCount = countLines(req.getCode());
        if (lineCount > properties.getMaxLines()) {
            throw new CodeTooLargeException(properties.getMaxLines());
        }

        // 1) 创建审查记录（占位 PENDING）
        Review review = new Review();
        review.setUserId(userId);
        review.setCodeContent(req.getCode());
        review.setFileName(req.getFileName());
        review.setLineCount(lineCount);
        review.setStatus("PENDING");
        review.setReviewTime(LocalDateTime.now());
        reviewRepository.insert(review);

        // 2) 同步执行引擎（MVP 阶段全量同步；异步放二期）
        try {
            long start = System.currentTimeMillis();
            List<Rule> rules = ruleService.listEnabled();
            ReviewContext ctx = reviewEngine.review(req.getCode(), req.getFileName(), rules);

            if (ctx.getCompilationUnit() == null && ctx.getIssues().isEmpty()) {
                // AST 解析失败 + 没有任何 REGEX 命中
                review.setStatus("FAILED");
                review.setErrorMsg("代码语法错误: " + ctx.getParseError());
                review.setCostMs(System.currentTimeMillis() - start);
                reviewRepository.updateById(review);
                throw new CodeParseException(ctx.getParseError() == null ? "未知错误" : ctx.getParseError());
            }

            // 3) 落库
            List<Issue> issues = persistIssues(review.getId(), ctx.getIssues());

            // 4) 异步 AI 解释（不阻塞主流程；失败也不影响）
            enrichWithAi(issues);

            // 5) 更新审查主表
            long cost = System.currentTimeMillis() - start;
            review.setStatus("COMPLETED");
            review.setIssueCount(issues.size());
            review.setCostMs(cost);
            reviewRepository.updateById(review);

            log.info("[REVIEW] id={} userId={} lines={} issues={} cost={}ms",
                    review.getId(), userId, lineCount, issues.size(), cost);

            return buildResultVO(review, issues);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("[REVIEW] 审查异常 reviewId=" + review.getId(), e);
            review.setStatus("FAILED");
            review.setErrorMsg("引擎内部错误: " + e.getMessage());
            reviewRepository.updateById(review);
            throw new BizException("审查失败: " + e.getMessage());
        }
    }

    @Override
    public ReviewResultVO detail(Long reviewId, Long currentUserId, String currentRole) {
        Review r = reviewRepository.selectById(reviewId);
        if (r == null) throw new BizException("审查记录不存在");
        if (!"ADMIN".equals(currentRole) && !currentUserId.equals(r.getUserId())) {
            // 教师可查看所有
            if (!"TEACHER".equals(currentRole)) {
                throw new ForbiddenException("无权查看他人审查记录");
            }
        }
        List<Issue> issues = issueRepository.selectList(
                new LambdaQueryWrapper<Issue>().eq(Issue::getReviewId, reviewId)
                        .orderByAsc(Issue::getLineNumber)
        );
        // 二次按严重级别排序
        issues.sort(Comparator.comparingInt(i -> severityWeight(i.getSeverity())));
        return buildResultVO(r, issues);
    }

    @Override
    public List<Review> history(Long userId, int current, int size) {
        return reviewRepository.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<Review>().eq(Review::getUserId, userId)
                        .orderByDesc(Review::getReviewTime)).getRecords();
    }

    @Override
    public List<Review> allHistory(int current, int size) {
        return reviewRepository.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<Review>().orderByDesc(Review::getReviewTime)).getRecords();
    }

    @Override
    public long countHistory(Long userId) {
        return reviewRepository.selectCount(new LambdaQueryWrapper<Review>().eq(Review::getUserId, userId));
    }

    @Override
    public long countAllHistory() {
        return reviewRepository.selectCount(null);
    }

    // ----------------- helpers -----------------

    private int countLines(String code) {
        if (code == null) return 0;
        // 去除 BOM
        if (!code.isEmpty() && code.charAt(0) == '\uFEFF') code = code.substring(1);
        if (code.isEmpty()) return 0;
        int n = 1;
        for (int i = 0; i < code.length(); i++) {
            if (code.charAt(i) == '\n') n++;
        }
        // 全部为空行
        if (code.endsWith("\n")) n--;
        return Math.max(n, 1);
    }

    private List<Issue> persistIssues(Long reviewId, List<IssueDraft> drafts) {
        List<Issue> entities = new ArrayList<>(drafts.size());
        for (IssueDraft d : drafts) {
            Issue e = d.toEntity(reviewId);
            issueRepository.insert(e);
            entities.add(e);
        }
        return entities;
    }

    @Async("reviewExecutor")
    public void enrichWithAi(List<Issue> issues) {
        for (Issue i : issues) {
            if (m3Client == null) continue;
            try {
                String explain = m3Client.explainIssue(i.getRuleName(), i.getDescription(), i.getCodeBefore());
                if (explain != null && !explain.isBlank()) {
                    i.setAiExplain(explain);
                    issueRepository.updateById(i);
                }
            } catch (Exception e) {
                log.debug("[M3] explain failed issueId={}", i.getId());
            }
        }
    }

    private int severityWeight(String s) {
        return switch (s == null ? "" : s) {
            case "CRITICAL" -> 0;
            case "ERROR" -> 1;
            case "WARNING" -> 2;
            case "SUGGESTION" -> 3;
            default -> 4;
        };
    }

    private ReviewResultVO buildResultVO(Review r, List<Issue> issues) {
        ReviewResultVO vo = new ReviewResultVO();
        vo.setReviewId(r.getId());
        vo.setFileName(r.getFileName());
        vo.setLineCount(r.getLineCount());
        vo.setIssueCount(issues.size());
        vo.setStatus(r.getStatus());
        vo.setCostMs(r.getCostMs());
        vo.setReviewTime(r.getReviewTime());

        long c = 0, e = 0, w = 0, s = 0;
        List<IssueVO> list = new ArrayList<>();
        List<IssueVO> secList = new ArrayList<>();
        for (Issue i : issues) {
            IssueVO v = IssueVO.from(i);
            list.add(v);
            switch (i.getSeverity()) {
                case "CRITICAL" -> { c++; if ("SECURITY".equals(i.getCategory())) secList.add(v); }
                case "ERROR" -> e++;
                case "WARNING" -> w++;
                case "SUGGESTION" -> s++;
            }
        }
        vo.setIssues(list);
        vo.setStats(new SeverityStats(c, e, w, s, (long) issues.size()));
        vo.setSecurityIssues(secList);
        return vo;
    }
}
