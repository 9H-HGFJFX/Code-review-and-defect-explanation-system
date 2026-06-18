package com.codeaudit.engine;

import com.codeaudit.entity.Issue;
import com.codeaudit.entity.Rule;
import com.codeaudit.engine.executor.RuleExecutor;
import com.codeaudit.engine.parser.JavaCodeParser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 审查引擎主控 - 概要设计 3.2.1
 * 流程：接收代码 → 预处理 → AST 解析 → 规范检查 → 缺陷检测 → 安全扫描 → 建议生成 → 报告组装
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewEngine {

    private final JavaCodeParser parser;
    private final ApplicationContext ctx;

    /** 执行器缓存：beanName -> executor */
    private final Map<String, RuleExecutor> executorMap = new ConcurrentHashMap<>();

    @Autowired
    private List<RuleExecutor> executors;

    @PostConstruct
    public void init() {
        for (RuleExecutor e : executors) {
            // 用小写 class name 作为 key，例如 namingChecker
            String key = e.getClass().getSimpleName();
            // 转 camelCase：NamingChecker -> namingChecker
            key = Character.toLowerCase(key.charAt(0)) + key.substring(1);
            executorMap.put(key, e);
        }
        log.info("[ENGINE] 已注册 {} 个规则执行器: {}", executorMap.size(), executorMap.keySet());
    }

    /**
     * 执行审查
     * @param sourceCode 原始代码
     * @param fileName 文件名
     * @param rules 当前启用的规则
     * @return 引擎上下文（含 AST、问题列表）
     */
    public ReviewContext review(String sourceCode, String fileName, List<Rule> rules) {
        ReviewContext context = parser.parse(sourceCode, fileName);
        if (context.getCompilationUnit() == null) {
            // 语法错误：安全类扫描仍可在源码上跑（REGEX 类不依赖 AST）
            log.warn("[ENGINE] 解析失败，仅运行 REGEX 类规则: {}", context.getParseError());
        }

        // 按规则执行
        for (Rule rule : rules) {
            if (rule.getEnabled() == null || rule.getEnabled() == 0) continue;
            String beanName = rule.getExecutorBean();
            if (beanName == null) continue;
            RuleExecutor exec = executorMap.get(beanName);
            if (exec == null) {
                log.warn("[ENGINE] 未找到执行器: {}", beanName);
                continue;
            }
            // 类型匹配校验
            if (!exec.category().equals(rule.getCategory())) {
                log.debug("[ENGINE] 规则 {} 的执行器 {} category 不匹配，跳过", rule.getCode(), beanName);
                continue;
            }
            try {
                exec.execute(context, rule);
            } catch (Exception e) {
                log.error("[ENGINE] 执行规则 {} 异常", rule.getCode(), e);
            }
        }

        // 排序
        context.getIssues().sort(Comparator.naturalOrder());
        return context;
    }

    /**
     * 引擎自检 - 启动时跑一个 hello world 验证 AST 解析可用
     */
    public boolean selfTest() {
        try {
            String sample = "public class HelloWorld {\n" +
                    "    public static void main(String[] args) {\n" +
                    "        System.out.println(\"Hello\");\n" +
                    "    }\n" +
                    "}";
            ReviewContext c = parser.parse(sample, "HelloWorld.java");
            return c.getCompilationUnit() != null;
        } catch (Exception e) {
            return false;
        }
    }

    public Map<String, RuleExecutor> getExecutors() {
        return Collections.unmodifiableMap(executorMap);
    }
}
