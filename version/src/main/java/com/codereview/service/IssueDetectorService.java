package com.codereview.service;

import com.codereview.engine.detector.DetectorResult;
import com.codereview.engine.parser.SourceFile;
import com.codereview.engine.rule.RuleEngine;
import com.codereview.engine.rule.RuleLoader;
import com.codereview.entity.CodeIssue;
import com.codereview.entity.ReviewRule;
import com.codereview.repository.CodeIssueMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 问题检测服务
 * 提供代码缺陷检测的核心功能
 * 
 * @author code-review-team
 */
@Service
@Slf4j
public class IssueDetectorService {

    /**
     * 规则引擎
     */
    @Autowired
    private RuleEngine ruleEngine;

    /**
     * 规则加载器
     */
    @Autowired
    private RuleLoader ruleLoader;

    /**
     * 问题Mapper
     */
    @Autowired
    private CodeIssueMapper issueMapper;

    /**
     * 单文件检测
     * 
     * @param file 源代码文件
     * @return 检测到的问题列表
     */
    public List<CodeIssue> detect(SourceFile file) {
        if (file == null) {
            return new ArrayList<>();
        }
        
        try {
            // 获取适用规则
            List<ReviewRule> rules = ruleEngine.getRulesByLanguage(file.getLanguage());
            
            // 执行规则匹配
            List<CodeIssue> issues = ruleEngine.match(file, rules);
            
            log.debug("单文件检测完成: path={}, 问题数={}", file.getPath(), issues.size());
            
            return issues;
            
        } catch (Exception e) {
            log.error("单文件检测失败: path={}, error={}", file.getPath(), e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 批量检测
     * 
     * @param files 文件列表
     * @param taskId 任务ID
     * @return 检测结果
     */
    public DetectorResult detectBatch(List<SourceFile> files, Long taskId) {
        if (files == null || files.isEmpty()) {
            log.info("待检测文件列表为空");
            return DetectorResult.complete(List.of(), List.of(), 
                new DetectorResult.ScanStats(0, 0, 0));
        }
        
        long startTime = System.currentTimeMillis();
        List<CodeIssue> allIssues = new ArrayList<>();
        List<DetectorResult.FailedFileInfo> failedFiles = new ArrayList<>();
        int processedCount = 0;
        
        log.info("开始批量检测: 文件总数={}, taskId={}", files.size(), taskId);
        
        // 确保规则已加载
        List<ReviewRule> allRules = ruleLoader.getLoadedRules();
        ruleEngine.loadRules(allRules);
        
        // 逐个文件处理（带异常隔离）
        for (SourceFile file : files) {
            try {
                // 获取适用规则
                List<ReviewRule> applicableRules = ruleEngine.getRulesByLanguage(file.getLanguage());
                
                // 执行规则匹配
                List<CodeIssue> issues = ruleEngine.match(file, applicableRules);
                allIssues.addAll(issues);
                processedCount++;
                
            } catch (Exception e) {
                // 单文件异常隔离，不影响其他文件
                log.error("文件处理异常: {}, error={}", file.getPath(), e.getMessage());
                failedFiles.add(new DetectorResult.FailedFileInfo(
                    file.getPath(),
                    com.codereview.common.enums.FailureReason.UNKNOWN_ERROR,
                    e.getMessage()
                ));
            }
        }
        
        long endTime = System.currentTimeMillis();
        
        // 构建统计信息
        DetectorResult.ScanStats stats = new DetectorResult.ScanStats(
            files.size(), processedCount, failedFiles.size(), startTime, endTime
        );
        stats.setTotalIssues(allIssues.size());
        
        log.info("批量检测完成: taskId={}, 总文件={}, 成功={}, 失败={}, 问题数={}, 耗时={}ms",
            taskId, files.size(), processedCount, failedFiles.size(), 
            allIssues.size(), endTime - startTime);
        
        return DetectorResult.complete(allIssues, failedFiles, stats);
    }

    /**
     * 保存检测结果
     * 
     * @param issues 问题列表
     * @param taskId 任务ID
     */
    public void saveIssues(List<CodeIssue> issues, Long taskId) {
        if (issues == null || issues.isEmpty()) {
            return;
        }
        
        for (CodeIssue issue : issues) {
            try {
                issue.setTaskId(taskId);
                issueMapper.insert(issue);
            } catch (Exception e) {
                log.error("保存问题失败: issue={}, error={}", issue, e.getMessage());
            }
        }
        
        log.info("保存问题完成: taskId={}, 问题数={}", taskId, issues.size());
    }
}
