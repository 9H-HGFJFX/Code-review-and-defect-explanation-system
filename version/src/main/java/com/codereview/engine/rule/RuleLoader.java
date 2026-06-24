package com.codereview.engine.rule;

import com.codereview.common.enums.FailMode;
import com.codereview.entity.ReviewRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 规则加载器
 * 负责从配置文件或数据库加载规则，实现fail-mode降级策略
 * 
 * 降级策略说明：
 * - fail-mode=log-and-skip：加载失败跳过并记录ERROR日志（默认）
 * - fail-mode=strict：加载失败抛出异常
 * - fail-mode=warn：加载失败记录WARN并跳过
 * 
 * @author code-review-team
 */
@Service
@Slf4j
public class RuleLoader {

    /**
     * 失败模式配置
     */
    @Value("${review.rule.fail-mode:log-and-skip}")
    private String failModeConfig;

    /**
     * 规则加载目录
     */
    @Value("${review.rule.rules-directory:classpath:rules}")
    private String rulesDirectory;

    /**
     * 失败模式枚举
     */
    private FailMode failMode;

    /**
     * 加载成功的规则列表
     */
    private final CopyOnWriteArrayList<ReviewRule> loadedRules = new CopyOnWriteArrayList<>();

    /**
     * 加载失败的规则信息
     */
    private final CopyOnWriteArrayList<RuleLoadResult.FailedRuleInfo> failedRules = new CopyOnWriteArrayList<>();

    /**
     * ObjectMapper用于解析JSON
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        // 解析失败模式
        this.failMode = FailMode.valueOf(failModeConfig.toUpperCase().replace("-", "_"));
        log.info("规则加载器初始化完成，失败模式: {}", failMode.getDescription());
        
        // 加载内置规则
        loadBuiltInRules();
    }

    /**
     * 加载规则
     * 
     * @param rulesDir 规则目录路径
     * @param mode 失败模式
     * @return 加载结果
     */
    public RuleLoadResult loadRules(Path rulesDir, FailMode mode) {
        FailMode currentMode = mode != null ? mode : this.failMode;
        RuleLoadResult result = new RuleLoadResult();
        List<ReviewRule> successList = new ArrayList<>();
        List<RuleLoadResult.FailedRuleInfo> failList = new ArrayList<>();

        // 如果目录不存在或为空，记录警告并继续
        if (rulesDir == null || !Files.exists(rulesDir)) {
            log.warn("规则目录不存在: {}", rulesDir);
            result.setLoadedRules(successList);
            result.setFailedRules(failList);
            result.setFailMode(currentMode);
            result.summarize();
            return result;
        }

        try {
            // 查找规则文件
            File[] ruleFiles = rulesDir.toFile().listFiles((dir, name) -> 
                name.endsWith(".yaml") || name.endsWith(".json") || name.endsWith(".yml")
            );

            if (ruleFiles == null || ruleFiles.length == 0) {
                log.info("规则目录中没有找到规则文件: {}", rulesDir);
                result.setLoadedRules(successList);
                result.setFailedRules(failList);
                result.setFailMode(currentMode);
                result.summarize();
                return result;
            }

            // 逐个加载规则文件
            for (File ruleFile : ruleFiles) {
                try {
                    ReviewRule rule = loadRuleFromFile(ruleFile);
                    if (rule != null) {
                        successList.add(rule);
                        loadedRules.add(rule);
                        log.debug("规则加载成功: {} ({})", rule.getName(), rule.getRuleId());
                    }
                } catch (Exception e) {
                    RuleLoadResult.FailedRuleInfo failedInfo = 
                        new RuleLoadResult.FailedRuleInfo(ruleFile.getName(), e.getMessage(), e);
                    failList.add(failedInfo);
                    failedRules.add(failedInfo);
                    
                    handleFailure(failedInfo, currentMode);
                }
            }

        } catch (Exception e) {
            log.error("规则加载失败: {}", e.getMessage(), e);
            if (currentMode == FailMode.STRICT) {
                throw new RuleLoadException("规则加载失败（strict模式）", e);
            }
        }

        result.setLoadedRules(successList);
        result.setFailedRules(failList);
        result.setFailMode(currentMode);
        result.summarize();

        log.info("规则加载完成: 成功={}, 失败={}, 总计={}", 
            successList.size(), failList.size(), successList.size() + failList.size());

        return result;
    }

    /**
     * 加载规则文件
     */
    private ReviewRule loadRuleFromFile(File file) throws Exception {
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        
        ReviewRule rule;
        if (file.getName().endsWith(".json")) {
            rule = objectMapper.readValue(content, ReviewRule.class);
        } else {
            // YAML格式，使用简化解析
            rule = parseYamlRule(content);
        }
        
        // 基本验证
        if (rule.getRuleId() == null || rule.getRuleId().isEmpty()) {
            throw new IllegalArgumentException("规则ID不能为空");
        }
        
        return rule;
    }

    /**
     * 解析YAML格式规则（简化实现）
     */
    private ReviewRule parseYamlRule(String content) {
        ReviewRule rule = new ReviewRule();
        
        // 简单解析：按行处理
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                String key = parts[0].trim();
                String value = parts.length > 1 ? parts[1].trim() : "";
                
                switch (key) {
                    case "ruleId":
                    case "id":
                        rule.setRuleId(value);
                        break;
                    case "name":
                        rule.setName(value);
                        break;
                    case "description":
                    case "desc":
                        rule.setDescription(value);
                        break;
                    case "category":
                        try {
                            rule.setCategory(com.codereview.common.enums.RuleCategory.valueOf(value.toUpperCase()));
                        } catch (Exception ignored) {}
                        break;
                    case "severity":
                        rule.setSeverity(value);
                        break;
                    case "languages":
                        rule.setLanguages(value);
                        break;
                    case "pattern":
                        rule.setPattern(value);
                        break;
                    case "enabled":
                        rule.setEnabled(Boolean.parseBoolean(value));
                        break;
                    case "priority":
                        try {
                            rule.setPriority(Integer.parseInt(value));
                        } catch (Exception ignored) {}
                        break;
                }
            }
        }
        
        // 默认值
        if (rule.getEnabled() == null) {
            rule.setEnabled(true);
        }
        if (rule.getPriority() == null) {
            rule.setPriority(100);
        }
        
        return rule;
    }

    /**
     * 处理加载失败
     */
    private void handleFailure(RuleLoadResult.FailedRuleInfo failedInfo, FailMode mode) {
        switch (mode) {
            case LOG_AND_SKIP:
                log.error("规则加载失败，跳过该规则: file={}, reason={}", 
                    failedInfo.getFileName(), failedInfo.getReason(), 
                    failedInfo.getStackTrace() != null ? 
                        new Throwable(failedInfo.getStackTrace()) : null);
                break;
            case STRICT:
                throw new RuleLoadException(
                    "规则加载失败（strict模式）: " + failedInfo.getFileName(), 
                    new Exception(failedInfo.getReason())
                );
            case WARN:
                log.warn("规则加载失败，跳过该规则: file={}, reason={}", 
                    failedInfo.getFileName(), failedInfo.getReason());
                break;
        }
    }

    /**
     * 加载内置规则
     */
    public void loadBuiltInRules() {
        log.info("加载内置规则...");
        
        // 安全规则
        addBuiltInRule("SEC-001", "SQL注入检测", 
            com.codereview.common.enums.RuleCategory.SECURITY,
            "检测字符串拼接SQL的潜在风险", "HIGH",
            "java,python,javascript", 
            "(\\\".*\\\\+\\\\s*\\\"|\\\\+\\\\s*\\\".*SELECT.*\\\")|(\\\\+\\\\s*\\\".*INSERT.*\\\")",
            true, 10);
        
        addBuiltInRule("SEC-002", "硬编码密码检测", 
            com.codereview.common.enums.RuleCategory.SECURITY,
            "检测代码中硬编码的密码或密钥", "CRITICAL",
            "java,python,javascript",
            "(password\\\\s*[=:][\\\\s]*[\\\"'][^\\\"']{1,50}[\\\"'])|(api[_-]?key\\\\s*[=:])|(secret\\\\s*[=:])",
            true, 10);
        
        addBuiltInRule("SEC-003", "XSS漏洞检测", 
            com.codereview.common.enums.RuleCategory.SECURITY,
            "检测未转义的用户输入直接输出", "HIGH",
            "java,javascript,python",
            "(innerHTML\\\\s*=)|(document\\\\.write\\\\s*\\\\()|(response\\\\.write\\\\s*\\\\()",
            true, 20);
        
        addBuiltInRule("SEC-004", "路径遍历检测", 
            com.codereview.common.enums.RuleCategory.SECURITY,
            "检测用户输入直接拼接到文件路径", "MEDIUM",
            "java,python,javascript",
            "(File\\\\s*\\\\(|new\\\\s+File\\\\(|open\\\\s*\\\\()",
            true, 30);
        
        // 代码风格规则
        addBuiltInRule("STYLE-001", "过长方法检测", 
            com.codereview.common.enums.RuleCategory.STYLE,
            "检测超过200行的方法", "MEDIUM",
            "java,python,javascript",
            null,
            true, 50);
        
        addBuiltInRule("STYLE-002", "魔法数字检测", 
            com.codereview.common.enums.RuleCategory.STYLE,
            "检测未定义常量的数字字面量", "LOW",
            "java,python,javascript",
            "[^0-9a-zA-Z]([1-9][0-9]{2,})[^0-9a-zA-Z]",
            true, 60);
        
        addBuiltInRule("STYLE-003", "重复代码检测", 
            com.codereview.common.enums.RuleCategory.STYLE,
            "检测代码片段重复（简化版）", "LOW",
            "java,python,javascript",
            null,
            true, 70);
        
        // 性能规则
        addBuiltInRule("PERF-001", "资源未关闭检测", 
            com.codereview.common.enums.RuleCategory.PERFORMANCE,
            "检测资源可能未正确关闭", "MEDIUM",
            "java",
            "(new\\\\s+FileInputStream|new\\\\s+FileOutputStream|new\\\\s+Connection)",
            true, 40);
        
        addBuiltInRule("PERF-002", "字符串拼接优化", 
            com.codereview.common.enums.RuleCategory.PERFORMANCE,
            "检测在循环中使用字符串拼接", "LOW",
            "java",
            "for\\\\s*\\\\([^)]*\\\\)\\\\s*\\\\{[^}]*\\\\+=",
            true, 80);
        
        // 最佳实践规则
        addBuiltInRule("BEST-001", "异常捕获检测", 
            com.codereview.common.enums.RuleCategory.BEST_PRACTICE,
            "检测空的异常捕获块", "MEDIUM",
            "java",
            "catch\\\\s*\\\\([^)]*\\\\)\\\\s*\\\\{\\\\s*\\\\}",
            true, 45);
        
        addBuiltInRule("BEST-002", "日志输出检测", 
            com.codereview.common.enums.RuleCategory.BEST_PRACTICE,
            "检测使用System.out进行日志输出", "LOW",
            "java",
            "System\\\\.(out|err)\\\\.(print|println)",
            true, 90);
        
        // 正确性规则
        addBuiltInRule("CORR-001", "空指针风险检测", 
            com.codereview.common.enums.RuleCategory.CORRECTNESS,
            "检测可能抛出空指针的操作", "HIGH",
            "java",
            "\\\\.toString\\\\(\\)|\\\\.equals\\\\(null\\)|\\\\.getClass\\\\(\\)",
            true, 15);
        
        log.info("内置规则加载完成: {}", loadedRules.size());
    }

    /**
     * 添加内置规则
     */
    private void addBuiltInRule(String ruleId, String name, 
                                  com.codereview.common.enums.RuleCategory category,
                                  String description, String severity,
                                  String languages, String pattern,
                                  boolean enabled, int priority) {
        ReviewRule rule = new ReviewRule();
        rule.setRuleId(ruleId);
        rule.setName(name);
        rule.setCategory(category);
        rule.setDescription(description);
        rule.setSeverity(severity);
        rule.setLanguages(languages);
        rule.setPattern(pattern);
        rule.setEnabled(enabled);
        rule.setPriority(priority);
        rule.setBuiltIn(true);
        
        loadedRules.add(rule);
    }

    /**
     * 获取已加载的规则列表
     */
    public List<ReviewRule> getLoadedRules() {
        return new ArrayList<>(loadedRules);
    }

    /**
     * 获取加载失败的规则信息
     */
    public List<RuleLoadResult.FailedRuleInfo> getFailedRules() {
        return new ArrayList<>(failedRules);
    }

    /**
     * 获取当前失败模式
     */
    public FailMode getFailMode() {
        return failMode;
    }

    /**
     * 设置失败模式
     */
    public void setFailMode(FailMode failMode) {
        this.failMode = failMode;
    }

    /**
     * 重新加载规则
     */
    public RuleLoadResult reload() {
        loadedRules.clear();
        failedRules.clear();
        loadBuiltInRules();
        
        try {
            Path rulesDir = Path.of(rulesDirectory);
            if (Files.exists(rulesDir)) {
                return loadRules(rulesDir, failMode);
            }
        } catch (Exception e) {
            log.warn("重新加载外部规则失败: {}", e.getMessage());
        }
        
        RuleLoadResult result = new RuleLoadResult();
        result.setLoadedRules(new ArrayList<>(loadedRules));
        result.setFailedRules(new ArrayList<>(failedRules));
        result.setFailMode(failMode);
        result.summarize();
        return result;
    }

    /**
     * 规则加载异常类
     */
    public static class RuleLoadException extends RuntimeException {
        public RuleLoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
