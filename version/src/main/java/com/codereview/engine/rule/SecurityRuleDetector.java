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
 * 安全规则检测器
 * 检测SQL注入、XSS、硬编码密码等安全漏洞
 * 
 * @author code-review-team
 */
@Component
public class SecurityRuleDetector implements RuleDetector {

    @Override
    public RuleCategory getCategory() {
        return RuleCategory.SECURITY;
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
            case "SEC-001":
            case "SEC-SQL-INJECTION":
                issues.addAll(detectSqlInjection(file, rule, content));
                break;
            case "SEC-002":
            case "SEC-HARDCODED-PASSWORD":
                issues.addAll(detectHardcodedPassword(file, rule, content));
                break;
            case "SEC-003":
            case "SEC-XSS":
                issues.addAll(detectXss(file, rule, content));
                break;
            case "SEC-004":
            case "SEC-PATH-TRAVERSAL":
                issues.addAll(detectPathTraversal(file, rule, content));
                break;
            default:
                // 使用规则的正则表达式模式
                if (rule.getPattern() != null && !rule.getPattern().isEmpty()) {
                    issues.addAll(matchByPattern(file, rule, content));
                }
                break;
        }

        return issues;
    }

    /**
     * SQL注入检测
     * 检测字符串拼接SQL、PreparedStatement误用等
     */
    private List<CodeIssue> detectSqlInjection(SourceFile file, ReviewRule rule, String content) {
        List<CodeIssue> issues = new ArrayList<>();
        
        // 模式1：字符串拼接SQL
        Pattern sqlConcat = Pattern.compile(
            "(\".*\\+\\s*\")|(\".*SELECT.*\\+\")|(\".*INSERT.*\\+\")|" +
            "(\".*UPDATE.*\\+\")|(\".*DELETE.*\\+\")|(\".*FROM.*\\+\")",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );
        
        Matcher matcher = sqlConcat.matcher(content);
        while (matcher.find()) {
            CodeIssue issue = createIssue(file, rule, matcher);
            issue.setDescription("检测到可能的SQL注入风险：使用字符串拼接构建SQL语句");
            issue.setSuggestion("建议使用参数化查询（PreparedStatement）或ORM框架");
            issues.add(issue);
        }
        
        // 模式2：未使用参数化查询
        if (content.contains("Statement") && content.contains("executeQuery")) {
            Pattern unsafeStatement = Pattern.compile(
                "Statement.*executeQuery.*\\+",
                Pattern.CASE_INSENSITIVE
            );
            Matcher m = unsafeStatement.matcher(content);
            while (m.find()) {
                CodeIssue issue = createIssue(file, rule, m);
                issue.setDescription("检测到使用Statement执行动态SQL");
                issue.setSuggestion("建议改用PreparedStatement进行参数化查询");
                issues.add(issue);
            }
        }
        
        return issues;
    }

    /**
     * 硬编码密码检测
     */
    private List<CodeIssue> detectHardcodedPassword(SourceFile file, ReviewRule rule, String content) {
        List<CodeIssue> issues = new ArrayList<>();
        
        // 检测密码硬编码
        Pattern passwordPattern = Pattern.compile(
            "(password\\s*[=:]\\s*[\"'][^\"']{3,}[\"'])" +
            "|(pwd\\s*[=:]\\s*[\"'][^\"']{3,}[\"'])" +
            "|(passwd\\s*[=:]\\s*[\"'][^\"']{3,}[\"'])" +
            "|(api[_-]?key\\s*[=:]\\s*[\"'][^\"']{3,}[\"'])" +
            "|(secret\\s*[=:]\\s*[\"'][^\"']{3,}[\"'])" +
            "|(token\\s*[=:]\\s*[\"'][^\"']{3,}[\"'])" +
            "|(auth\\s*[=:]\\s*[\"'][^\"']{3,}[\"'])" +
            "|(private[_-]?key\\s*[=:]\\s*[\"'][^\"']{3,}[\"'])",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = passwordPattern.matcher(content);
        while (matcher.find()) {
            CodeIssue issue = createIssue(file, rule, matcher);
            issue.setSeverity(IssueSeverity.CRITICAL);
            issue.setDescription("检测到硬编码的敏感信息：密码、密钥或Token");
            issue.setSuggestion("建议将敏感信息移至配置文件、环境变量或密钥管理系统");
            issues.add(issue);
        }
        
        return issues;
    }

    /**
     * XSS漏洞检测
     */
    private List<CodeIssue> detectXss(SourceFile file, ReviewRule rule, String content) {
        List<CodeIssue> issues = new ArrayList<>();
        
        // 检测直接HTML输出
        Pattern xssPatterns = Pattern.compile(
            "(innerHTML\\s*=)" +
            "|(document\\.write\\s*\\()" +
            "|(response\\.write\\s*\\()" +
            "|(\\.html\\s*\\([^,)]*request)" +
            "|(\\.append\\s*\\([^,)]*request)" +
            "|(out\\.print\\s*\\([^)]*request)" +
            "|(echo\\s+[^;]*\\$_)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = xssPatterns.matcher(content);
        while (matcher.find()) {
            CodeIssue issue = createIssue(file, rule, matcher);
            issue.setSeverity(IssueSeverity.HIGH);
            issue.setDescription("检测到可能的XSS风险：未进行输入转义直接输出");
            issue.setSuggestion("建议对用户输入进行HTML转义或使用安全的模板引擎");
            issues.add(issue);
        }
        
        return issues;
    }

    /**
     * 路径遍历检测
     */
    private List<CodeIssue> detectPathTraversal(SourceFile file, ReviewRule rule, String content) {
        List<CodeIssue> issues = new ArrayList<>();
        
        // 检测用户输入直接拼接到文件路径
        Pattern pathPatterns = Pattern.compile(
            "(new\\s+File\\s*\\([^)]*request)" +
            "|(FileInputStream\\s*\\([^)]*request)" +
            "|(open\\s*\\([^)]*request)" +
            "|(readFile\\s*\\([^)]*request)" +
            "|(include\\s*\\([^)]*request)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = pathPatterns.matcher(content);
        while (matcher.find()) {
            CodeIssue issue = createIssue(file, rule, matcher);
            issue.setDescription("检测到可能的路径遍历风险：用户输入直接拼接到文件路径");
            issue.setSuggestion("建议对用户输入进行路径规范化验证，禁止包含../等路径遍历字符");
            issues.add(issue);
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
                CodeIssue issue = createIssue(file, rule, matcher);
                issues.add(issue);
            }
        } catch (Exception e) {
            // 忽略无效的正则表达式
        }
        
        return issues;
    }

    /**
     * 创建问题实体
     */
    private CodeIssue createIssue(SourceFile file, ReviewRule rule, Matcher matcher) {
        CodeIssue issue = new CodeIssue();
        issue.setFilePath(file.getPath());
        issue.setLineNumber(calculateLineNumber(file.getContent(), matcher.start()));
        issue.setColumnNumber(calculateColumn(file.getContent(), matcher.start()));
        issue.setRuleId(rule.getRuleId());
        issue.setRuleName(rule.getName());
        issue.setCategory(rule.getCategory());
        issue.setSeverity(IssueSeverity.valueOf(rule.getSeverity()));
        issue.setDescription(rule.getDescription());
        issue.setCodeSnippet(matcher.group());
        issue.setSuggestion(rule.getDescription());
        issue.setStatus(IssueStatus.NEW);
        return issue;
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
