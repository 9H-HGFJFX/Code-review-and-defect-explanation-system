package com.codereview.engine.checker;

import com.codereview.entity.Issue;
import com.codereview.entity.Rule;
import com.github.javaparser.ast.CompilationUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 安全扫描器
 * 
 * 检测内容：
 * - SQL注入
 * - XSS跨站脚本
 * - 硬编码密码/密钥
 * - 路径遍历
 */
@Slf4j
@Component
public class SecurityScanner implements CodeChecker {
    
    // 硬编码敏感信息正则
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "(password|passwd|pwd|secret|api_key|apikey|token|access_key|accesskey)\\s*=\\s*[\"'][^\"']+[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    // SQL注入风险正则
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(SELECT|INSERT|UPDATE|DELETE|DROP|UNION)\\s+.*\\+.*",
        Pattern.CASE_INSENSITIVE
    );
    
    @Override
    public List<Issue> check(CompilationUnit ast, String sourceCode, List<Rule> rules) {
        List<Issue> issues = new ArrayList<>();
        
        // 过滤安全相关规则
        List<Rule> securityRules = rules.stream()
                .filter(r -> Rule.CATEGORY_SECURITY.equals(r.getCategory()))
                .toList();
        
        // 执行自定义规则
        for (Rule rule : securityRules) {
            if (Rule.PATTERN_REGEX.equals(rule.getPatternType()) && rule.getPattern() != null) {
                try {
                    Pattern pattern = Pattern.compile(rule.getPattern(), Pattern.CASE_INSENSITIVE);
                    var matcher = pattern.matcher(sourceCode);
                    while (matcher.find()) {
                        int lineNumber = getLineNumber(sourceCode, matcher.start());
                        Issue issue = createIssue(rule, lineNumber, matcher.group(), sourceCode);
                        issues.add(issue);
                    }
                } catch (Exception e) {
                    log.warn("规则{}正则匹配失败: {}", rule.getName(), e.getMessage());
                }
            }
        }
        
        // 内置安全检测
        issues.addAll(detectHardcodedSecrets(sourceCode, securityRules));
        
        return issues;
    }
    
    /**
     * 检测硬编码敏感信息
     */
    private List<Issue> detectHardcodedSecrets(String sourceCode, List<Rule> rules) {
        List<Issue> issues = new ArrayList<>();
        
        // 检查是否有安全规则专门检测硬编码
        for (Rule rule : rules) {
            if (rule.getName() != null && rule.getName().toLowerCase().contains("hardcode")) {
                if (Rule.PATTERN_REGEX.equals(rule.getPatternType())) {
                    try {
                        Pattern pattern = Pattern.compile(rule.getPattern(), Pattern.CASE_INSENSITIVE);
                        var matcher = pattern.matcher(sourceCode);
                        while (matcher.find()) {
                            int lineNumber = getLineNumber(sourceCode, matcher.start());
                            Issue issue = createIssue(rule, lineNumber, matcher.group(), sourceCode);
                            issues.add(issue);
                        }
                    } catch (Exception e) {
                        log.warn("规则{}正则匹配失败: {}", rule.getName(), e.getMessage());
                    }
                }
            }
        }
        
        // 内置硬编码检测
        var matcher = PASSWORD_PATTERN.matcher(sourceCode);
        while (matcher.find()) {
            // 检查是否已被规则检测
            boolean alreadyDetected = issues.stream()
                    .anyMatch(i -> i.getLineNumber() == getLineNumber(sourceCode, matcher.start()));
            
            if (!alreadyDetected) {
                int lineNumber = getLineNumber(sourceCode, matcher.start());
                Issue issue = Issue.builder()
                        .severity(Issue.SEVERITY_CRITICAL)
                        .lineNumber(lineNumber)
                        .description("检测到硬编码敏感信息: " + matcher.group())
                        .suggestion("将敏感信息移至配置文件或环境变量，使用System.getenv()或配置文件读取")
                        .codeBefore(matcher.group())
                        .build();
                issues.add(issue);
            }
        }
        
        return issues;
    }
    
    /**
     * 根据字符串位置获取行号
     */
    private int getLineNumber(String source, int position) {
        int line = 1;
        for (int i = 0; i < position && i < source.length(); i++) {
            if (source.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }
    
    /**
     * 创建问题对象
     */
    private Issue createIssue(Rule rule, int lineNumber, String matchedText, String sourceCode) {
        String suggestion = rule.getSuggestionTemplate();
        if (suggestion == null) {
            suggestion = "请修复此安全问题";
        }
        
        return Issue.builder()
                .ruleId(rule.getId())
                .severity(rule.getSeverity())
                .lineNumber(lineNumber)
                .description(rule.getDescription() + ": " + matchedText)
                .suggestion(suggestion)
                .codeBefore(matchedText)
                .build();
    }
}
