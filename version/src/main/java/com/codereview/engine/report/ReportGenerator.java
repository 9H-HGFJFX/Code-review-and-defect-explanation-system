package com.codereview.engine.report;

import com.codereview.common.enums.IssueSeverity;
import com.codereview.entity.CodeIssue;
import com.codereview.entity.ReviewReport;
import com.codereview.entity.ReviewTask;
import com.codereview.repository.CodeIssueMapper;
import com.codereview.repository.ReviewReportMapper;
import com.codereview.repository.ReviewTaskMapper;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 报告生成器
 * 生成HTML、JSON等格式的代码审查报告
 * 
 * @author code-review-team
 */
@Service
@Slf4j
public class ReportGenerator {

    @Autowired
    private ReviewTaskMapper taskMapper;

    @Autowired
    private CodeIssueMapper issueMapper;

    @Autowired
    private ReviewReportMapper reportMapper;

    /**
     * 报告存储目录
     */
    @Value("${review.report.storage-directory:/app/reports}")
    private String storageDirectory;

    /**
     * 生成HTML报告
     * 
     * @param taskId 任务ID
     * @return 报告文件路径
     */
    public String generateHtml(Long taskId) {
        try {
            ReviewTask task = taskMapper.selectById(taskId);
            if (task == null) {
                log.error("任务不存在: taskId={}", taskId);
                return null;
            }
            
            List<CodeIssue> issues = issueMapper.selectByTaskId(taskId);
            
            // 生成HTML内容
            String html = buildHtmlReport(task, issues);
            
            // 保存文件
            String fileName = String.format("report_%d_%s.html", 
                taskId, 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            );
            Path filePath = saveReport(html, fileName);
            
            // 保存报告记录
            saveReportRecord(taskId, fileName, filePath.toString(), "HTML", html.length());
            
            log.info("HTML报告生成完成: taskId={}, path={}", taskId, filePath);
            return filePath.toString();
            
        } catch (Exception e) {
            log.error("生成HTML报告失败: taskId={}, error={}", taskId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 生成JSON报告
     * 
     * @param taskId 任务ID
     * @return 报告文件路径
     */
    public String generateJson(Long taskId) {
        try {
            ReviewTask task = taskMapper.selectById(taskId);
            if (task == null) {
                log.error("任务不存在: taskId={}", taskId);
                return null;
            }
            
            List<CodeIssue> issues = issueMapper.selectByTaskId(taskId);
            
            // 构建JSON结构
            Map<String, Object> reportData = buildJsonReport(task, issues);
            String json = JSON.toJSONString(reportData, com.alibaba.fastjson2.JSONWriter.Feature.PrettyFormat);
            
            // 保存文件
            String fileName = String.format("report_%d_%s.json", 
                taskId, 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            );
            Path filePath = saveReport(json, fileName);
            
            // 保存报告记录
            saveReportRecord(taskId, fileName, filePath.toString(), "JSON", (long) json.length());
            
            log.info("JSON报告生成完成: taskId={}, path={}", taskId, filePath);
            return filePath.toString();
            
        } catch (Exception e) {
            log.error("生成JSON报告失败: taskId={}, error={}", taskId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 按文件分组
     * 
     * @param taskId 任务ID
     * @return 文件分组的问题列表
     */
    public Map<String, List<CodeIssue>> groupByFile(Long taskId) {
        List<CodeIssue> issues = issueMapper.selectByTaskId(taskId);
        
        return issues.stream()
            .collect(Collectors.groupingBy(
                CodeIssue::getFilePath,
                LinkedHashMap::new,
                Collectors.toList()
            ));
    }

    /**
     * 按严重程度分组
     * 
     * @param taskId 任务ID
     * @return 严重程度分组的问题列表
     */
    public Map<IssueSeverity, List<CodeIssue>> groupBySeverity(Long taskId) {
        List<CodeIssue> issues = issueMapper.selectByTaskId(taskId);
        
        return issues.stream()
            .collect(Collectors.groupingBy(
                CodeIssue::getSeverity,
                LinkedHashMap::new,
                Collectors.toList()
            ));
    }

    /**
     * 按规则类别分组
     * 
     * @param taskId 任务ID
     * @return 类别分组的问题列表
     */
    public Map<String, List<CodeIssue>> groupByCategory(Long taskId) {
        List<CodeIssue> issues = issueMapper.selectByTaskId(taskId);
        
        return issues.stream()
            .collect(Collectors.groupingBy(
                issue -> issue.getCategory() != null ? issue.getCategory().name() : "UNKNOWN",
                LinkedHashMap::new,
                Collectors.toList()
            ));
    }

    /**
     * 构建HTML报告
     */
    private String buildHtmlReport(ReviewTask task, List<CodeIssue> issues) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html lang='zh-CN'>\n");
        sb.append("<head>\n");
        sb.append("    <meta charset='UTF-8'>\n");
        sb.append("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        sb.append("    <title>代码审查报告 - ").append(task.getTitle()).append("</title>\n");
        sb.append("    <style>\n");
        sb.append(getHtmlStyles());
        sb.append("    </style>\n");
        sb.append("</head>\n");
        sb.append("<body>\n");
        
        // 头部信息
        sb.append(buildHeader(task));
        
        // 统计概览
        sb.append(buildStatsOverview(issues));
        
        // 严重程度分布
        sb.append(buildSeverityChart(issues));
        
        // 问题详情列表
        sb.append(buildIssueList(issues));
        
        sb.append("</body>\n");
        sb.append("</html>");
        
        return sb.toString();
    }

    /**
     * HTML样式
     */
    private String getHtmlStyles() {
        return """
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                background-color: #f5f7fa;
                color: #333;
                line-height: 1.6;
            }
            .container {
                max-width: 1200px;
                margin: 0 auto;
                padding: 20px;
            }
            .header {
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                color: white;
                padding: 30px;
                border-radius: 10px;
                margin-bottom: 20px;
            }
            .header h1 {
                font-size: 28px;
                margin-bottom: 10px;
            }
            .header .meta {
                opacity: 0.9;
                font-size: 14px;
            }
            .stats {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                gap: 15px;
                margin-bottom: 20px;
            }
            .stat-card {
                background: white;
                padding: 20px;
                border-radius: 10px;
                box-shadow: 0 2px 8px rgba(0,0,0,0.08);
            }
            .stat-card .label {
                color: #666;
                font-size: 14px;
                margin-bottom: 5px;
            }
            .stat-card .value {
                font-size: 32px;
                font-weight: bold;
            }
            .stat-card.critical .value { color: #d32f2f; }
            .stat-card.high .value { color: #f57c00; }
            .stat-card.medium .value { color: #fbc02d; }
            .stat-card.low .value { color: #388e3c; }
            .severity-chart {
                background: white;
                padding: 20px;
                border-radius: 10px;
                margin-bottom: 20px;
            }
            .severity-bar {
                display: flex;
                height: 30px;
                border-radius: 5px;
                overflow: hidden;
                margin: 15px 0;
            }
            .severity-bar .critical { background: #d32f2f; }
            .severity-bar .high { background: #f57c00; }
            .severity-bar .medium { background: #fbc02d; }
            .severity-bar .low { background: #388e3c; }
            .severity-bar .info { background: #1976d2; }
            .issue-list {
                background: white;
                border-radius: 10px;
                overflow: hidden;
            }
            .issue-item {
                padding: 15px 20px;
                border-bottom: 1px solid #eee;
            }
            .issue-item:last-child {
                border-bottom: none;
            }
            .issue-header {
                display: flex;
                align-items: center;
                margin-bottom: 10px;
            }
            .severity-badge {
                padding: 4px 10px;
                border-radius: 4px;
                font-size: 12px;
                font-weight: bold;
                color: white;
                margin-right: 10px;
            }
            .severity-badge.critical { background: #d32f2f; }
            .severity-badge.high { background: #f57c00; }
            .severity-badge.medium { background: #fbc02d; color: #333; }
            .severity-badge.low { background: #388e3c; }
            .severity-badge.info { background: #1976d2; }
            .issue-location {
                color: #666;
                font-size: 13px;
            }
            .issue-description {
                margin-bottom: 10px;
            }
            .issue-code {
                background: #f5f5f5;
                padding: 10px;
                border-radius: 5px;
                font-family: 'Monaco', 'Menlo', monospace;
                font-size: 13px;
                overflow-x: auto;
                white-space: pre-wrap;
                word-break: break-all;
            }
            .issue-suggestion {
                color: #1976d2;
                font-size: 13px;
                margin-top: 10px;
            }
            """;
    }

    /**
     * 构建头部
     */
    private String buildHeader(ReviewTask task) {
        return String.format("""
            <div class='header'>
                <h1>%s</h1>
                <div class='meta'>
                    <span>任务ID: %d</span> |
                    <span>创建时间: %s</span> |
                    <span>状态: %s</span>
                </div>
            </div>
            """,
            escapeHtml(task.getTitle()),
            task.getId(),
            task.getCreateTime(),
            task.getStatus()
        );
    }

    /**
     * 构建统计概览
     */
    private String buildStatsOverview(List<CodeIssue> issues) {
        Map<IssueSeverity, Long> countMap = issues.stream()
            .collect(Collectors.groupingBy(CodeIssue::getSeverity, Collectors.counting()));
        
        long critical = countMap.getOrDefault(IssueSeverity.CRITICAL, 0L);
        long high = countMap.getOrDefault(IssueSeverity.HIGH, 0L);
        long medium = countMap.getOrDefault(IssueSeverity.MEDIUM, 0L);
        long low = countMap.getOrDefault(IssueSeverity.LOW, 0L);
        
        return String.format("""
            <div class='stats'>
                <div class='stat-card'>
                    <div class='label'>总问题数</div>
                    <div class='value'>%d</div>
                </div>
                <div class='stat-card critical'>
                    <div class='label'>严重</div>
                    <div class='value'>%d</div>
                </div>
                <div class='stat-card high'>
                    <div class='label'>高危</div>
                    <div class='value'>%d</div>
                </div>
                <div class='stat-card medium'>
                    <div class='label'>中等</div>
                    <div class='value'>%d</div>
                </div>
                <div class='stat-card low'>
                    <div class='label'>低</div>
                    <div class='value'>%d</div>
                </div>
            </div>
            """, issues.size(), critical, high, medium, low);
    }

    /**
     * 构建严重程度图表
     */
    private String buildSeverityChart(List<CodeIssue> issues) {
        long total = issues.size();
        if (total == 0) {
            return "<div class='severity-chart'><h3>问题分布</h3><p>没有问题</p></div>";
        }
        
        Map<IssueSeverity, Long> countMap = issues.stream()
            .collect(Collectors.groupingBy(CodeIssue::getSeverity, Collectors.counting()));
        
        double criticalPct = countMap.getOrDefault(IssueSeverity.CRITICAL, 0L) * 100.0 / total;
        double highPct = countMap.getOrDefault(IssueSeverity.HIGH, 0L) * 100.0 / total;
        double mediumPct = countMap.getOrDefault(IssueSeverity.MEDIUM, 0L) * 100.0 / total;
        double lowPct = countMap.getOrDefault(IssueSeverity.LOW, 0L) * 100.0 / total;
        double infoPct = countMap.getOrDefault(IssueSeverity.INFO, 0L) * 100.0 / total;
        
        return String.format("""
            <div class='severity-chart'>
                <h3>问题严重程度分布</h3>
                <div class='severity-bar'>
                    <div class='critical' style='width:%.1f%%'></div>
                    <div class='high' style='width:%.1f%%'></div>
                    <div class='medium' style='width:%.1f%%'></div>
                    <div class='low' style='width:%.1f%%'></div>
                    <div class='info' style='width:%.1f%%'></div>
                </div>
            </div>
            """, criticalPct, highPct, mediumPct, lowPct, infoPct);
    }

    /**
     * 构建问题列表
     */
    private String buildIssueList(List<CodeIssue> issues) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='issue-list'><h3 style='padding: 15px 20px; border-bottom: 1px solid #eee;'>问题详情</h3>");
        
        if (issues.isEmpty()) {
            sb.append("<div class='issue-item'><p style='text-align:center; color:#666;'>没有问题发现</p></div>");
        } else {
            for (CodeIssue issue : issues) {
                String severityClass = issue.getSeverity() != null ? 
                    issue.getSeverity().name().toLowerCase() : "info";
                String severityLabel = issue.getSeverity() != null ? 
                    issue.getSeverity().getDescription() : "提示";
                
                sb.append(String.format("""
                    <div class='issue-item'>
                        <div class='issue-header'>
                            <span class='severity-badge %s'>%s</span>
                            <span class='issue-location'>%s:%d</span>
                        </div>
                        <div class='issue-description'>%s</div>
                        <div class='issue-code'>%s</div>
                        <div class='issue-suggestion'>修复建议: %s</div>
                    </div>
                    """,
                    severityClass,
                    severityLabel,
                    escapeHtml(issue.getFilePath()),
                    issue.getLineNumber(),
                    escapeHtml(issue.getDescription()),
                    escapeHtml(issue.getCodeSnippet()),
                    escapeHtml(issue.getSuggestion())
                ));
            }
        }
        
        sb.append("</div>");
        return sb.toString();
    }

    /**
     * 构建JSON报告
     */
    private Map<String, Object> buildJsonReport(ReviewTask task, List<CodeIssue> issues) {
        Map<String, Object> report = new LinkedHashMap<>();
        
        // 任务基本信息
        report.put("taskId", task.getId());
        report.put("taskName", task.getTitle());
        report.put("status", task.getStatus());
        report.put("createTime", task.getCreateTime());
        report.put("durationSeconds", task.getDurationSeconds());
        
        // 统计信息
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalIssues", issues.size());
        stats.put("criticalCount", issues.stream().filter(i -> i.getSeverity() == IssueSeverity.CRITICAL).count());
        stats.put("highCount", issues.stream().filter(i -> i.getSeverity() == IssueSeverity.HIGH).count());
        stats.put("mediumCount", issues.stream().filter(i -> i.getSeverity() == IssueSeverity.MEDIUM).count());
        stats.put("lowCount", issues.stream().filter(i -> i.getSeverity() == IssueSeverity.LOW).count());
        stats.put("infoCount", issues.stream().filter(i -> i.getSeverity() == IssueSeverity.INFO).count());
        report.put("statistics", stats);
        
        // 问题列表
        report.put("issues", issues);
        
        // 按文件分组
        Map<String, List<Map<String, Object>>> byFile = new LinkedHashMap<>();
        for (CodeIssue issue : issues) {
            byFile.computeIfAbsent(issue.getFilePath(), k -> new ArrayList<>())
                .add(Map.of(
                    "lineNumber", issue.getLineNumber(),
                    "severity", issue.getSeverity(),
                    "description", issue.getDescription()
                ));
        }
        report.put("issuesByFile", byFile);
        
        return report;
    }

    /**
     * 保存报告文件
     */
    private Path saveReport(String content, String fileName) throws IOException {
        // 确保目录存在
        Path dir = Paths.get(storageDirectory);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        
        Path filePath = dir.resolve(fileName);
        Files.writeString(filePath, content, StandardCharsets.UTF_8);
        
        return filePath;
    }

    /**
     * 保存报告记录
     */
    private void saveReportRecord(Long taskId, String title, String filePath, String reportType, long fileSize) {
        try {
            ReviewReport report = new ReviewReport();
            report.setTaskId(taskId);
            report.setTitle(title);
            report.setFilePath(filePath);
            report.setReportType(reportType);
            report.setFileSize(fileSize);
            report.setGenerateTime(LocalDateTime.now());
            report.setCreateTime(LocalDateTime.now());
            
            reportMapper.insert(report);
        } catch (Exception e) {
            log.error("保存报告记录失败: error={}", e.getMessage());
        }
    }

    /**
     * HTML转义
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
