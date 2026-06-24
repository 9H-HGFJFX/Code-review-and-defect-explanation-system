package com.codereview.engine.rule;

import com.codereview.common.enums.IssueSeverity;
import com.codereview.common.enums.IssueStatus;
import com.codereview.common.enums.RuleCategory;
import com.codereview.engine.parser.SourceFile;
import com.codereview.entity.CodeIssue;
import com.codereview.entity.ReviewRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 风格规则检测器
 * 检测过长方法、重复代码、魔法数字等代码风格问题
 * 
 * @author code-review-team
 */
@Component
public class StyleRuleDetector implements RuleDetector {

    /**
     * 默认过长方法阈值（行数）
     */
    private static final int DEFAULT_LONG_METHOD_THRESHOLD = 200;
    
    /**
     * 默认魔法数字阈值（大于此值视为魔法数字）
     */
    private static final int DEFAULT_MAGIC_NUMBER_THRESHOLD = 99;

    @Override
    public RuleCategory getCategory() {
        return RuleCategory.STYLE;
    }

    @Override
    public List<CodeIssue> detect(SourceFile file, ReviewRule rule) {
        List<CodeIssue> issues = new ArrayList<>();
        String content = file.getContent();
        String ruleId = rule.getRuleId();

        if (content == null || content.isEmpty()) {
            return issues;
        }

        // 根据规则ID执行特定检测
        switch (ruleId != null ? ruleId : "") {
            case "STYLE-001":
            case "STYLE-LONG-METHOD":
                issues.addAll(detectLongMethod(file, rule, content));
                break;
            case "STYLE-002":
            case "STYLE-MAGIC-NUMBER":
                issues.addAll(detectMagicNumber(file, rule, content));
                break;
            case "STYLE-003":
            case "STYLE-DUPLICATE-CODE":
                issues.addAll(detectDuplicateCode(file, rule, content));
                break;
            default:
                if (rule.getPattern() != null && !rule.getPattern().isEmpty()) {
                    issues.addAll(matchByPattern(file, rule, content));
                }
                break;
        }

        return issues;
    }

    /**
     * 过长方法检测
     */
    private List<CodeIssue> detectLongMethod(SourceFile file, ReviewRule rule, String content) {
        List<CodeIssue> issues = new ArrayList<>();
        
        // 从规则配置获取阈值
        int threshold = getThreshold(rule, DEFAULT_LONG_METHOD_THRESHOLD);
        
        // 计算文件行数
        int lineCount = (int) content.lines().count();
        
        if (lineCount > threshold) {
            CodeIssue issue = new CodeIssue();
            issue.setFilePath(file.getPath());
            issue.setLineNumber(1);
            issue.setRuleId(rule.getRuleId());
            issue.setRuleName(rule.getName());
            issue.setCategory(rule.getCategory());
            issue.setSeverity(IssueSeverity.valueOf(rule.getSeverity()));
            issue.setDescription(String.format("文件过长：共 %d 行，超过阈值 %d 行", lineCount, threshold));
            issue.setSuggestion("建议将文件拆分为多个模块或类，每个文件保持单一职责");
            issue.setCodeSnippet("文件总行数: " + lineCount);
            issue.setStatus(IssueStatus.NEW);
            issues.add(issue);
        }
        
        return issues;
    }

    /**
     * 魔法数字检测
     */
    private List<CodeIssue> detectMagicNumber(SourceFile file, ReviewRule rule, String content) {
        List<CodeIssue> issues = new ArrayList<>();
        
        // 从规则配置获取阈值
        int threshold = getThreshold(rule, DEFAULT_MAGIC_NUMBER_THRESHOLD);
        
        // 检测魔法数字：大于阈值的数字常量（排除科学计数法、小数等）
        Pattern magicNumberPattern = Pattern.compile(
            "[^0-9a-zA-Z](" + threshold + "[0-9]{1,})[^0-9a-zA-Z_.]"
        );
        
        Matcher matcher = magicNumberPattern.matcher(content);
        int matchCount = 0;
        int maxReports = 5; // 限制报告数量
        
        while (matcher.find() && matchCount < maxReports) {
            String matchedNumber = matcher.group(1);
            int lineNumber = calculateLineNumber(content, matcher.start());
            
            CodeIssue issue = new CodeIssue();
            issue.setFilePath(file.getPath());
            issue.setLineNumber(lineNumber);
            issue.setColumnNumber(calculateColumn(content, matcher.start()));
            issue.setRuleId(rule.getRuleId());
            issue.setRuleName(rule.getName());
            issue.setCategory(rule.getCategory());
            issue.setSeverity(IssueSeverity.valueOf(rule.getSeverity()));
            issue.setDescription("检测到魔法数字：" + matchedNumber);
            issue.setSuggestion("建议定义常量来代替魔法数字，提高代码可读性和可维护性");
            issue.setCodeSnippet("数字常量: " + matchedNumber);
            issue.setStatus(IssueStatus.NEW);
            issues.add(issue);
            matchCount++;
        }
        
        return issues;
    }

    /**
     * 重复代码检测（简化版）
     * 实际生产中可以使用更复杂的相似度算法
     */
    private List<CodeIssue> detectDuplicateCode(SourceFile file, ReviewRule rule, String content) {
        List<CodeIssue> issues = new ArrayList<>();
        
        // 简化实现：检测连续重复的行
        String[] lines = content.split("\n");
        StringBuilder repeatedBlock = new StringBuilder();
        int startLine = -1;
        int repeatCount = 0;
        int minRepeatLines = 4; // 最少重复4行才报告
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            // 跳过空行和注释
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("#") || 
                line.startsWith("/*") || line.startsWith("*")) {
                continue;
            }
            
            // 简化检测：检查是否有连续相同的非空行
            if (repeatedBlock.length() > 0 && repeatedBlock.toString().equals(line)) {
                if (startLine == -1) {
                    startLine = i - 1;
                }
                repeatCount++;
            } else {
                if (repeatCount >= minRepeatLines) {
                    // 报告重复
                    CodeIssue issue = new CodeIssue();
                    issue.setFilePath(file.getPath());
                    issue.setLineNumber(startLine + 1);
                    issue.setRuleId(rule.getRuleId());
                    issue.setRuleName(rule.getName());
                    issue.setCategory(rule.getCategory());
                    issue.setSeverity(IssueSeverity.valueOf(rule.getSeverity()));
                    issue.setDescription("检测到可能重复的代码片段");
                    issue.setSuggestion("建议提取为公共方法或常量");
                    issue.setCodeSnippet("重复代码: " + repeatedBlock);
                    issue.setStatus(IssueStatus.NEW);
                    issues.add(issue);
                }
                repeatedBlock = new StringBuilder(line);
                repeatCount = 1;
                startLine = -1;
            }
        }
        
        return issues;
    }

    /**
     * 根据正则表达式模式匹配
     */
    private List<CodeIssue> matchByPattern(SourceFile file, ReviewRule rule, String content) {
        List<CodeIssue> issues = new ArrayList<>();
        
        try {
            Pattern pattern = Pattern.compile(rule.getPattern(), 
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(content);
            
            while (matcher.find()) {
                CodeIssue issue = new CodeIssue();
                issue.setFilePath(file.getPath());
                issue.setLineNumber(calculateLineNumber(content, matcher.start()));
                issue.setColumnNumber(calculateColumn(content, matcher.start()));
                issue.setRuleId(rule.getRuleId());
                issue.setRuleName(rule.getName());
                issue.setCategory(rule.getCategory());
                issue.setSeverity(IssueSeverity.valueOf(rule.getSeverity()));
                issue.setDescription(rule.getDescription());
                issue.setCodeSnippet(matcher.group());
                issue.setSuggestion(rule.getDescription());
                issue.setStatus(IssueStatus.NEW);
                issues.add(issue);
            }
        } catch (Exception e) {
            // 忽略无效的正则表达式
        }
        
        return issues;
    }

    /**
     * 从规则配置获取阈值
     */
    private int getThreshold(ReviewRule rule, int defaultValue) {
        if (rule.getConfig() == null || rule.getConfig().isEmpty()) {
            return defaultValue;
        }
        
        try {
            String config = rule.getConfig();
            // 简单解析：查找数字
            String numStr = config.replaceAll("[^0-9]", "");
            if (!numStr.isEmpty()) {
                return Integer.parseInt(numStr);
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        
        return defaultValue;
    }

    /**
     * 计算行号
     */
    private int calculateLineNumber(String content, int position) {
        int line = 1;
        for (int i = 0; i < position && i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    /**
     * 计算列号
     */
    private int calculateColumn(String content, int position) {
        int lastNewLine = content.lastIndexOf('\n', position);
        if (lastNewLine < 0) {
            return position + 1;
        }
        return position - lastNewLine;
    }
}
