package com.codereview.engine;

import com.codereview.entity.Issue;
import com.codereview.entity.Rule;
import com.codereview.engine.checker.*;
import com.codereview.exception.CodeSyntaxException;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

/**
 * 代码审查引擎核心类
 * 
 * 工作流程：
 * 1. 接收代码文本
 * 2. JavaParser AST解析
 * 3. 安全扫描（最高优先级）
 * 4. 缺陷检测
 * 5. 规范检查
 * 6. 生成修复建议
 * 7. 组装审查报告
 */
@Slf4j
@Component
public class ReviewEngine {
    
    @Autowired
    private SecurityScanner securityScanner;
    
    @Autowired
    private DefectDetector defectDetector;
    
    @Autowired
    private StyleChecker styleChecker;
    
    private final JavaParser javaParser;
    
    private final ExecutorService executor;
    
    public ReviewEngine() {
        this.javaParser = new JavaParser();
        this.executor = Executors.newFixedThreadPool(4);
    }
    
    /**
     * 执行代码审查
     * 
     * @param code 源代码
     * @param rules 应用的规则列表
     * @param timeoutSeconds 超时时间（秒）
     * @return 审查结果
     */
    public ReviewResponse review(String code, List<Rule> rules, int timeoutSeconds) {
        List<Issue> allIssues = new ArrayList<>();
        
        try {
            // 1. 预处理：检测代码行数
            int lineCount = (int) code.lines().count();
            log.info("开始审查代码，行数: {}", lineCount);
            
            // 2. AST解析
            CompilationUnit ast = parseCode(code);
            if (ast == null) {
                return ReviewResponse.error("代码语法错误，无法解析");
            }
            
            // 3. 安全扫描（最高优先级）
            List<Issue> securityIssues = executeWithTimeout(
                () -> securityScanner.scan(ast, code, rules),
                timeoutSeconds,
                TimeUnit.SECONDS
            );
            if (securityIssues != null) {
                allIssues.addAll(securityIssues);
            }
            
            // 4. 缺陷检测
            List<Issue> defectIssues = executeWithTimeout(
                () -> defectDetector.detect(ast, code, rules),
                timeoutSeconds,
                TimeUnit.SECONDS
            );
            if (defectIssues != null) {
                allIssues.addAll(defectIssues);
            }
            
            // 5. 规范检查
            List<Issue> styleIssues = executeWithTimeout(
                () -> styleChecker.check(ast, code, rules),
                timeoutSeconds,
                TimeUnit.SECONDS
            );
            if (styleIssues != null) {
                allIssues.addAll(styleIssues);
            }
            
            // 6. 按严重级别排序
            allIssues.sort(Comparator.comparing(this::getSeverityPriority));
            
            log.info("审查完成，发现{}个问题", allIssues.size());
            return ReviewResponse.success(allIssues, code, lineCount);
            
        } catch (TimeoutException e) {
            log.warn("代码审查超时");
            return ReviewResponse.error("代码审查超时，请尝试减少代码行数");
        } catch (Exception e) {
            log.error("代码审查异常", e);
            return ReviewResponse.error("审查过程发生异常: " + e.getMessage());
        }
    }
    
    /**
     * 解析Java代码为AST
     */
    private CompilationUnit parseCode(String code) {
        try {
            // 预处理：移除BOM头
            if (code.startsWith("\uFEFF")) {
                code = code.substring(1);
            }
            
            ParseResult<CompilationUnit> result = javaParser.parse(code);
            if (result.isSuccessful() && result.getResult().isPresent()) {
                return result.getResult().get();
            } else {
                StringBuilder errors = new StringBuilder();
                result.getProblems().forEach(p -> errors.append(p.getVerboseMessage()).append("; "));
                throw new CodeSyntaxException(errors.toString());
            }
        } catch (CodeSyntaxException e) {
            throw e;
        } catch (Exception e) {
            throw new CodeSyntaxException(e.getMessage());
        }
    }
    
    /**
     * 带超时的任务执行
     */
    private <T> T executeWithTimeout(Callable<T> task, long timeout, TimeUnit unit) throws TimeoutException {
        Future<T> future = executor.submit(task);
        try {
            return future.get(timeout, unit);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            log.error("任务执行异常", e.getCause());
            return null;
        }
    }
    
    /**
     * 获取严重级别优先级（数字越小优先级越高）
     */
    private int getSeverityPriority(Issue issue) {
        return switch (issue.getSeverity()) {
            case Issue.SEVERITY_CRITICAL -> 1;
            case Issue.SEVERITY_ERROR -> 2;
            case Issue.SEVERITY_WARNING -> 3;
            case Issue.SEVERITY_SUGGESTION -> 4;
            default -> 5;
        };
    }
}
