package com.codeaudit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codeaudit.entity.Issue;
import com.codeaudit.entity.Review;
import com.codeaudit.repository.IssueRepository;
import com.codeaudit.repository.ReviewRepository;
import com.codeaudit.service.StatisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatisticServiceImpl implements StatisticService {

    private final ReviewRepository reviewRepository;
    private final IssueRepository issueRepository;

    @Override
    public Map<String, Object> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalReviews", reviewRepository.selectCount(null));
        data.put("totalIssues", issueRepository.selectCount(null));

        // 严重级别分布
        List<Issue> all = issueRepository.selectList(null);
        Map<String, Long> severityDist = new LinkedHashMap<>();
        severityDist.put("CRITICAL", 0L);
        severityDist.put("ERROR", 0L);
        severityDist.put("WARNING", 0L);
        severityDist.put("SUGGESTION", 0L);
        for (Issue i : all) {
            severityDist.merge(i.getSeverity(), 1L, Long::sum);
        }
        data.put("severityDistribution", severityDist);

        // 分类分布
        Map<String, Long> categoryDist = new LinkedHashMap<>();
        categoryDist.put("STYLE", 0L);
        categoryDist.put("DEFECT", 0L);
        categoryDist.put("SECURITY", 0L);
        for (Issue i : all) {
            categoryDist.merge(i.getCategory(), 1L, Long::sum);
        }
        data.put("categoryDistribution", categoryDist);

        // 最近 7 天审查量（按天）
        Map<String, Long> trend = new LinkedHashMap<>();
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(6);
        List<Review> recent = reviewRepository.selectList(
                new LambdaQueryWrapper<Review>().ge(Review::getReviewTime, weekAgo)
        );
        // 初始化最近 7 天
        for (int i = 6; i >= 0; i--) {
            String day = LocalDateTime.now().minusDays(i).toLocalDate().toString();
            trend.put(day, 0L);
        }
        for (Review r : recent) {
            String day = r.getReviewTime().toLocalDate().toString();
            trend.merge(day, 1L, Long::sum);
        }
        data.put("trend7Days", trend);

        // 高频问题 Top 10
        Map<String, Long> freq = new HashMap<>();
        for (Issue i : all) {
            String key = i.getCategory() + "::" + (i.getRuleName() == null ? "未知" : i.getRuleName());
            freq.merge(key, 1L, Long::sum);
        }
        List<Map<String, Object>> top = new ArrayList<>();
        freq.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .forEach(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("key", e.getKey());
                    m.put("count", e.getValue());
                    top.add(m);
                });
        data.put("topIssues", top);

        return data;
    }
}
