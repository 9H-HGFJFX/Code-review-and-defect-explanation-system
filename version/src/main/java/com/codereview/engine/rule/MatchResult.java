package com.codereview.engine.rule;

import com.codereview.engine.parser.ParseResult;
import com.codereview.engine.parser.SourceFile;
import com.codereview.entity.CodeIssue;
import com.codereview.entity.ReviewRule;
import lombok.Data;

import java.util.List;

/**
 * 规则匹配结果封装类
 * 包含规则匹配后的详细信息
 * 
 * @author code-review-team
 */
@Data
public class MatchResult {
    
    /**
     * 是否匹配成功
     */
    private boolean matched;
    
    /**
     * 匹配到的规则
     */
    private ReviewRule rule;
    
    /**
     * 匹配到的文件
     */
    private SourceFile sourceFile;
    
    /**
     * 匹配到的代码片段
     */
    private String matchedCode;
    
    /**
     * 问题行号
     */
    private Integer lineNumber;
    
    /**
     * 问题列号
     */
    private Integer columnNumber;
    
    /**
     * 问题描述
     */
    private String description;
    
    /**
     * 修复建议
     */
    private String suggestion;
    
    /**
     * 严重程度（从规则继承或动态计算）
     */
    private String severity;
    
    /**
     * 创建匹配成功结果
     */
    public static MatchResult matched(ReviewRule rule, SourceFile sourceFile, 
                                       String matchedCode, int lineNumber) {
        MatchResult result = new MatchResult();
        result.setMatched(true);
        result.setRule(rule);
        result.setSourceFile(sourceFile);
        result.setMatchedCode(matchedCode);
        result.setLineNumber(lineNumber);
        result.setSeverity(rule.getSeverity());
        return result;
    }

    /**
     * 创建未匹配结果
     */
    public static MatchResult notMatched() {
        MatchResult result = new MatchResult();
        result.setMatched(false);
        return result;
    }

    /**
     * 转换为CodeIssue实体
     */
    public CodeIssue toCodeIssue(Long taskId) {
        CodeIssue issue = new CodeIssue();
        issue.setTaskId(taskId);
        issue.setFilePath(sourceFile.getPath());
        issue.setLineNumber(lineNumber);
        issue.setRuleId(rule.getRuleId());
        issue.setRuleName(rule.getName());
        issue.setCategory(rule.getCategory());
        issue.setSeverity(com.codereview.common.enums.IssueSeverity.valueOf(
            severity != null ? severity : rule.getSeverity()));
        issue.setDescription(description != null ? description : rule.getDescription());
        issue.setCodeSnippet(matchedCode);
        issue.setSuggestion(suggestion != null ? suggestion : rule.getDescription());
        issue.setStatus(com.codereview.common.enums.IssueStatus.NEW);
        return issue;
    }
}
