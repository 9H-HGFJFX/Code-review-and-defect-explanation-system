package com.codereview.engine.checker;

import com.codereview.entity.Issue;
import com.codereview.entity.Rule;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 缺陷检测器
 * 
 * 检测内容：
 * - 空指针风险
 * - 资源泄漏
 * - 逻辑错误（永真/永假条件）
 * - 死循环风险
 */
@Slf4j
@Component
public class DefectDetector implements CodeChecker {
    
    @Override
    public List<Issue> check(CompilationUnit ast, String sourceCode, List<Rule> rules) {
        List<Issue> issues = new ArrayList<>();
        
        // 过滤缺陷相关规则
        List<Rule> defectRules = rules.stream()
                .filter(r -> Rule.CATEGORY_DEFECT.equals(r.getCategory()))
                .toList();
        
        // 执行规则
        for (Rule rule : defectRules) {
            if (Rule.PATTERN_REGEX.equals(rule.getPatternType()) && rule.getPattern() != null) {
                try {
                    java.util.regex.Pattern pattern = 
                        java.util.regex.Pattern.compile(rule.getPattern(), java.util.regex.Pattern.CASE_INSENSITIVE);
                    var matcher = pattern.matcher(sourceCode);
                    while (matcher.find()) {
                        int lineNumber = getLineNumber(sourceCode, matcher.start());
                        Issue issue = createIssue(rule, lineNumber, matcher.group());
                        issues.add(issue);
                    }
                } catch (Exception e) {
                    log.warn("规则{}匹配失败: {}", rule.getName(), e.getMessage());
                }
            }
        }
        
        // 内置缺陷检测
        issues.addAll(detectPotentialNullPointer(ast, sourceCode, defectRules));
        issues.addAll(detectUnclosedResources(ast, sourceCode, defectRules));
        
        return issues;
    }
    
    /**
     * 检测潜在的空指针
     */
    private List<Issue> detectPotentialNullPointer(CompilationUnit ast, String sourceCode, List<Rule> rules) {
        List<Issue> issues = new ArrayList<>();
        
        // 访问方法调用表达式
        new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodCallExpr method, Void arg) {
                super.visit(method, arg);
                
                String methodName = method.getNameAsString();
                // 检测可能返回null的方法未做非空判断
                if (isRiskyMethod(methodName) && !hasNullCheck(method, ast)) {
                    Issue issue = Issue.builder()
                            .severity(Issue.SEVERITY_WARNING)
                            .lineNumber(method.getBegin().map(p -> p.line).orElse(0))
                            .description("方法" + methodName + "可能返回null，调用前建议进行非空判断")
                            .suggestion("在使用前添加 null 检查，如: if (" + methodName + "() != null)")
                            .build();
                    issues.add(issue);
                }
            }
        }.visit(ast, null);
        
        return issues;
    }
    
    /**
     * 检测未关闭的资源
     */
    private List<Issue> detectUnclosedResources(CompilationUnit ast, String sourceCode, List<Rule> rules) {
        List<Issue> issues = new ArrayList<>();
        
        new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodCallExpr method, Void arg) {
                super.visit(method, arg);
                
                String methodName = method.getNameAsString();
                // 检测需要关闭的资源
                if (isResourceMethod(methodName) && !isInTryWithResources(method, ast)) {
                    Issue issue = Issue.builder()
                            .severity(Issue.SEVERITY_WARNING)
                            .lineNumber(method.getBegin().map(p -> p.line).orElse(0))
                            .description("资源" + methodName + "可能未正确关闭")
                            .suggestion("建议使用 try-with-resources 语句确保资源正确关闭")
                            .build();
                    issues.add(issue);
                }
            }
        }.visit(ast, null);
        
        return issues;
    }
    
    /**
     * 判断是否为风险方法
     */
    private boolean isRiskyMethod(String methodName) {
        return methodName.startsWith("find") || 
               methodName.startsWith("get") ||
               methodName.equals("next") ||
               methodName.equals("toString");
    }
    
    /**
     * 判断是否有null检查
     */
    private boolean hasNullCheck(MethodCallExpr method, CompilationUnit ast) {
        // 简化实现，实际需要更复杂的控制流分析
        return false;
    }
    
    /**
     * 判断是否为资源方法
     */
    private boolean isResourceMethod(String methodName) {
        return methodName.contains("InputStream") || 
               methodName.contains("OutputStream") ||
               methodName.contains("Reader") ||
               methodName.contains("Writer") ||
               methodName.contains("Connection");
    }
    
    /**
     * 判断是否在try-with-resources中
     */
    private boolean isInTryWithResources(MethodCallExpr method, CompilationUnit ast) {
        // 简化实现
        return false;
    }
    
    /**
     * 获取行号
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
    private Issue createIssue(Rule rule, int lineNumber, String matchedText) {
        return Issue.builder()
                .ruleId(rule.getId())
                .severity(rule.getSeverity())
                .lineNumber(lineNumber)
                .description(rule.getDescription() + ": " + matchedText)
                .suggestion(rule.getSuggestionTemplate())
                .codeBefore(matchedText)
                .build();
    }
}
