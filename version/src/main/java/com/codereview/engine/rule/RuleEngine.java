package com.codereview.engine.rule;

import com.codereview.common.enums.ProgrammingLanguage;
import com.codereview.common.enums.RuleCategory;
import com.codereview.engine.parser.SourceFile;
import com.codereview.entity.CodeIssue;
import com.codereview.entity.ReviewRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 规则引擎核心类
 * 负责根据语言类型匹配适用规则并执行检测
 * 
 * @author code-review-team
 */
@Service
@Slf4j
public class RuleEngine {

    /**
     * 规则缓存（语言 -> 规则列表）
     */
    private final Map<String, List<ReviewRule>> rulesByLanguage = new HashMap<>();
    
    /**
     * 规则缓存（类别 -> 规则列表）
     */
    private final Map<RuleCategory, List<ReviewRule>> rulesByCategory = new HashMap<>();
    
    /**
     * 规则缓存（规则ID -> 规则）
     */
    private final Map<String, ReviewRule> rulesById = new HashMap<>();
    
    /**
     * 规则列表（原始顺序）
     */
    private volatile List<ReviewRule> allRules = new ArrayList<>();

    /**
     * 加载规则列表到引擎
     * 
     * @param rules 规则列表
     */
    public void loadRules(List<ReviewRule> rules) {
        if (rules == null || rules.isEmpty()) {
            log.warn("规则列表为空，跳过加载");
            return;
        }
        
        this.allRules = new ArrayList<>(rules);
        
        // 按语言分类
        rulesByLanguage.clear();
        for (ReviewRule rule : rules) {
            if (rule.getLanguages() != null) {
                String[] languages = rule.getLanguages().split(",");
                for (String lang : languages) {
                    String trimmedLang = lang.trim().toLowerCase();
                    rulesByLanguage.computeIfAbsent(trimmedLang, k -> new ArrayList<>()).add(rule);
                }
            }
        }
        
        // 按类别分类
        rulesByCategory.clear();
        for (ReviewRule rule : rules) {
            if (rule.getCategory() != null) {
                rulesByCategory.computeIfAbsent(rule.getCategory(), k -> new ArrayList<>()).add(rule);
            }
        }
        
        // 按ID索引
        rulesById.clear();
        for (ReviewRule rule : rules) {
            rulesById.put(rule.getRuleId(), rule);
        }
        
        log.info("规则引擎加载完成: 总规则数={}, 语言分类={}, 类别分类={}", 
            rules.size(), rulesByLanguage.size(), rulesByCategory.size());
    }

    /**
     * 根据源代码文件匹配适用规则
     * 
     * @param file 源代码文件
     * @param rules 候选规则列表
     * @return 匹配到的缺陷列表
     */
    public List<CodeIssue> match(SourceFile file, List<ReviewRule> rules) {
        if (file == null || rules == null || rules.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<CodeIssue> issues = new ArrayList<>();
        ProgrammingLanguage language = file.getLanguage();
        
        // 过滤适用于当前语言的规则
        List<ReviewRule> applicableRules = filterApplicableRules(file, rules);
        
        log.debug("文件 {} 适用规则数量: {}", file.getPath(), applicableRules.size());
        
        // 对每条规则执行匹配
        for (ReviewRule rule : applicableRules) {
            try {
                Optional<CodeIssue> issue = matchRule(file, rule);
                issue.ifPresent(issues::add);
            } catch (Exception e) {
                log.error("规则 {} 匹配失败: {}", rule.getRuleId(), e.getMessage(), e);
            }
        }
        
        return issues;
    }

    /**
     * 单条规则匹配
     * 
     * @param file 源代码文件
     * @param rule 规则
     * @return 匹配到的缺陷（如果匹配）
     */
    public Optional<CodeIssue> matchRule(SourceFile file, ReviewRule rule) {
        if (file == null || rule == null || !rule.getEnabled()) {
            return Optional.empty();
        }
        
        String content = file.getContent();
        if (content == null || content.isEmpty()) {
            return Optional.empty();
        }
        
        try {
            // 获取规则配置
            String pattern = rule.getPattern();
            String config = rule.getConfig();
            
            // 优先使用正则表达式匹配
            if (pattern != null && !pattern.isEmpty()) {
                return matchByRegex(file, rule, pattern);
            }
            
            // 根据规则类型进行特定匹配
            return matchByRuleType(file, rule);
            
        } catch (Exception e) {
            log.error("规则 {} 匹配执行失败: {}", rule.getRuleId(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 根据正则表达式匹配
     */
    private Optional<CodeIssue> matchByRegex(SourceFile file, ReviewRule rule, String pattern) {
        try {
            Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            var matcher = regex.matcher(file.getContent());
            
            if (matcher.find()) {
                CodeIssue issue = createIssueFromMatch(file, rule, matcher);
                return Optional.of(issue);
            }
        } catch (Exception e) {
            log.warn("正则表达式匹配失败: pattern={}, error={}", pattern, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * 根据规则类型进行特定匹配
     */
    private Optional<CodeIssue> matchByRuleType(SourceFile file, ReviewRule rule) {
        // 根据规则ID或类别进行特定检测
        String ruleId = rule.getRuleId();
        
        if (ruleId != null) {
            // 安全规则检测
            if (ruleId.startsWith("SEC-") || rule.getCategory() == RuleCategory.SECURITY) {
                return detectSecurityIssue(file, rule);
            }
            // 风格规则检测
            else if (ruleId.startsWith("STYLE-") || rule.getCategory() == RuleCategory.STYLE) {
                return detectStyleIssue(file, rule);
            }
        }
        
        return Optional.empty();
    }

    /**
     * 安全问题检测
     */
    private Optional<CodeIssue> detectSecurityIssue(SourceFile file, ReviewRule rule) {
        String content = file.getContent();
        String ruleId = rule.getRuleId();
        
        // SQL注入检测
        if ("SEC-001".equals(ruleId) || "SEC-SQL-INJECTION".equals(ruleId)) {
            return detectSqlInjection(file, rule, content);
        }
        
        // 硬编码密码检测
        if ("SEC-002".equals(ruleId) || "SEC-HARDCODED-PASSWORD".equals(ruleId)) {
            return detectHardcodedPassword(file, rule, content);
        }
        
        // XSS检测
        if ("SEC-003".equals(ruleId) || "SEC-XSS".equals(ruleId)) {
            return detectXss(file, rule, content);
        }
        
        return Optional.empty();
    }

    /**
     * 风格问题检测
     */
    private Optional<CodeIssue> detectStyleIssue(SourceFile file, ReviewRule rule) {
        String ruleId = rule.getRuleId();
        String content = file.getContent();
        
        // 过长方法检测
        if ("STYLE-001".equals(ruleId) || "STYLE-LONG-METHOD".equals(ruleId)) {
            return detectLongMethod(file, rule);
        }
        
        // 魔法数字检测
        if ("STYLE-002".equals(ruleId) || "STYLE-MAGIC-NUMBER".equals(ruleId)) {
            return detectMagicNumber(file, rule, content);
        }
        
        return Optional.empty();
    }

    /**
     * SQL注入检测
     */
    private Optional<CodeIssue> detectSqlInjection(SourceFile file, ReviewRule rule, String content) {
        // 检测字符串拼接SQL的模式
        Pattern sqlConcatPattern = Pattern.compile(
            "(\".*\\+\\s*\\\"|\\\".*\\\".*\\+\\s*)" +
            "|(\\+\\s*\\\".*SELECT.*\\\")" +
            "|(\\+\\s*\\\".*INSERT.*\\\")" +
            "|(\\+\\s*\\\".*UPDATE.*\\\")" +
            "|(\\+\\s*\\\".*DELETE.*\\\")",
            Pattern.CASE_INSENSITIVE
        );
        
        var matcher = sqlConcatPattern.matcher(content);
        if (matcher.find()) {
            CodeIssue issue = createIssueFromMatch(file, rule, matcher);
            issue.setDescription("检测到可能的SQL注入风险：使用字符串拼接构建SQL语句");
            issue.setSuggestion("建议使用参数化查询或预编译语句");
            return Optional.of(issue);
        }
        return Optional.empty();
    }

    /**
     * 硬编码密码检测
     */
    private Optional<CodeIssue> detectHardcodedPassword(SourceFile file, ReviewRule rule, String content) {
        Pattern passwordPattern = Pattern.compile(
            "(password\\s*[=:]\\s*[\"'][^\"']{1,50}[\"'])" +
            "|(api[_-]?key\\s*[=:]\\s*[\"'][^\"']{1,50}[\"'])" +
            "|(secret\\s*[=:]\\s*[\"'][^\"']{1,50}[\"'])" +
            "|(token\\s*[=:]\\s*[\"'][^\"']{1,50}[\"'])",
            Pattern.CASE_INSENSITIVE
        );
        
        var matcher = passwordPattern.matcher(content);
        if (matcher.find()) {
            CodeIssue issue = createIssueFromMatch(file, rule, matcher);
            issue.setDescription("检测到硬编码的敏感信息");
            issue.setSuggestion("建议将敏感信息移至配置文件或环境变量");
            return Optional.of(issue);
        }
        return Optional.empty();
    }

    /**
     * XSS检测
     */
    private Optional<CodeIssue> detectXss(SourceFile file, ReviewRule rule, String content) {
        // 检测直接输出用户输入而未转义的情况
        Pattern xssPattern = Pattern.compile(
            "(response\\.write\\s*\\()|" +
            "(innerHTML\\s*=)|" +
            "(document\\.write\\s*\\()|" +
            "(\\.html\\s*\\([^)]*request)",
            Pattern.CASE_INSENSITIVE
        );
        
        var matcher = xssPattern.matcher(content);
        if (matcher.find()) {
            CodeIssue issue = createIssueFromMatch(file, rule, matcher);
            issue.setDescription("检测到可能的XSS风险：未进行输入转义直接输出");
            issue.setSuggestion("建议对用户输入进行HTML转义处理");
            return Optional.of(issue);
        }
        return Optional.empty();
    }

    /**
     * 过长方法检测
     */
    private Optional<CodeIssue> detectLongMethod(SourceFile file, ReviewRule rule) {
        // 从规则配置中获取阈值，默认200行
        int threshold = 200;
        if (rule.getConfig() != null) {
            try {
                // 简单解析配置
                String config = rule.getConfig();
                if (config.contains("threshold")) {
                    String numStr = config.replaceAll("[^0-9]", "");
                    if (!numStr.isEmpty()) {
                        threshold = Integer.parseInt(numStr);
                    }
                }
            } catch (Exception e) {
                log.warn("解析规则配置失败: {}", e.getMessage());
            }
        }
        
        if (file.getLineCount() != null && file.getLineCount() > threshold) {
            CodeIssue issue = new CodeIssue();
            issue.setFilePath(file.getPath());
            issue.setLineNumber(1);
            issue.setRuleId(rule.getRuleId());
            issue.setRuleName(rule.getName());
            issue.setCategory(rule.getCategory());
            issue.setSeverity(com.codereview.common.enums.IssueSeverity.valueOf(rule.getSeverity()));
            issue.setDescription("文件过长：共 " + file.getLineCount() + " 行，超过阈值 " + threshold + " 行");
            issue.setSuggestion("建议将文件拆分为多个模块或类");
            issue.setStatus(com.codereview.common.enums.IssueStatus.NEW);
            return Optional.of(issue);
        }
        return Optional.empty();
    }

    /**
     * 魔法数字检测
     */
    private Optional<CodeIssue> detectMagicNumber(SourceFile file, ReviewRule rule, String content) {
        // 检测数字常量（排除常见的0, 1, -1等）
        Pattern magicNumberPattern = Pattern.compile(
            "[^0-9a-zA-Z]([1-9][0-9]{2,})[^0-9a-zA-Z]" // 匹配大于99的数字
        );
        
        var matcher = magicNumberPattern.matcher(content);
        if (matcher.find()) {
            CodeIssue issue = createIssueFromMatch(file, rule, matcher);
            issue.setDescription("检测到魔法数字：" + matcher.group(1));
            issue.setSuggestion("建议定义常量来代替魔法数字，提高代码可读性");
            return Optional.of(issue);
        }
        return Optional.empty();
    }

    /**
     * 过滤适用于指定文件的规则
     */
    private List<ReviewRule> filterApplicableRules(SourceFile file, List<ReviewRule> rules) {
        ProgrammingLanguage language = file.getLanguage();
        if (language == null) {
            return rules.stream()
                .filter(r -> r.getLanguages() == null || r.getLanguages().isEmpty())
                .collect(Collectors.toList());
        }
        
        String langCode = language.getCode().toLowerCase();
        
        return rules.stream()
            .filter(ReviewRule::getEnabled) // 只选择启用的规则
            .filter(r -> {
                if (r.getLanguages() == null || r.getLanguages().isEmpty()) {
                    return true; // 不限制语言的规则适用
                }
                String[] langs = r.getLanguages().toLowerCase().split(",");
                for (String lang : langs) {
                    if (lang.trim().equals(langCode) || lang.trim().equals("all")) {
                        return true;
                    }
                }
                return false;
            })
            .sorted(Comparator.comparing(r -> r.getPriority() != null ? r.getPriority() : 100))
            .collect(Collectors.toList());
    }

    /**
     * 根据匹配结果创建问题实体
     */
    private CodeIssue createIssueFromMatch(SourceFile file, ReviewRule rule, java.util.regex.Matcher matcher) {
        CodeIssue issue = new CodeIssue();
        issue.setFilePath(file.getPath());
        
        // 计算行号
        int lineNumber = calculateLineNumber(file.getContent(), matcher.start());
        issue.setLineNumber(lineNumber);
        
        // 计算列号
        issue.setColumnNumber(calculateColumn(file.getContent(), matcher.start()));
        
        issue.setRuleId(rule.getRuleId());
        issue.setRuleName(rule.getName());
        issue.setCategory(rule.getCategory());
        issue.setSeverity(com.codereview.common.enums.IssueSeverity.valueOf(rule.getSeverity()));
        issue.setDescription(rule.getDescription());
        issue.setCodeSnippet(matcher.group());
        issue.setSuggestion(rule.getDescription());
        issue.setStatus(com.codereview.common.enums.IssueStatus.NEW);
        
        return issue;
    }

    /**
     * 计算匹配位置的行号
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
     * 计算匹配位置的列号
     */
    private int calculateColumn(String content, int position) {
        int lastNewLine = content.lastIndexOf('\n', position);
        if (lastNewLine < 0) {
            return position + 1;
        }
        return position - lastNewLine;
    }

    /**
     * 获取指定语言的规则列表
     */
    public List<ReviewRule> getRulesByLanguage(ProgrammingLanguage language) {
        if (language == null) {
            return Collections.emptyList();
        }
        return rulesByLanguage.getOrDefault(language.getCode().toLowerCase(), Collections.emptyList());
    }

    /**
     * 获取指定类别的规则列表
     */
    public List<ReviewRule> getRulesByCategory(RuleCategory category) {
        if (category == null) {
            return Collections.emptyList();
        }
        return rulesByCategory.getOrDefault(category, Collections.emptyList());
    }

    /**
     * 获取所有已加载的规则
     */
    public List<ReviewRule> getAllRules() {
        return new ArrayList<>(allRules);
    }

    /**
     * 根据规则ID获取规则
     */
    public ReviewRule getRuleById(String ruleId) {
        return rulesById.get(ruleId);
    }
}
