package com.codereview.engine.checker;

import com.codereview.entity.Issue;
import com.codereview.entity.Rule;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 代码规范检查器
 * 
 * 检测内容：
 * - 命名规范
 * - 代码格式
 * - 注释完整性
 * - 代码结构
 */
@Slf4j
@Component
public class StyleChecker implements CodeChecker {
    
    // 驼峰命名检查
    private static final java.util.regex.Pattern CAMEL_CASE = 
        java.util.regex.Pattern.compile("^[a-z][a-zA-Z0-9]*$");
    
    // 帕斯卡命名检查（类名）
    private static final java.util.regex.Pattern PASCAL_CASE = 
        java.util.regex.Pattern.compile("^[A-Z][a-zA-Z0-9]*$");
    
    // 常量命名检查
    private static final java.util.regex.Pattern CONSTANT_CASE = 
        java.util.regex.Pattern.compile("^[A-Z][A-Z0-9_]*$");
    
    @Override
    public List<Issue> check(CompilationUnit ast, String sourceCode, List<Rule> rules) {
        List<Issue> issues = new ArrayList<>();
        
        // 过滤规范相关规则
        List<Rule> styleRules = rules.stream()
                .filter(r -> Rule.CATEGORY_STYLE.equals(r.getCategory()))
                .toList();
        
        // 执行规则
        for (Rule rule : styleRules) {
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
        
        // 内置规范检测
        issues.addAll(checkNaming(ast));
        issues.addAll(checkMethodLength(ast));
        
        return issues;
    }
    
    /**
     * 检查命名规范
     */
    private List<Issue> checkNaming(CompilationUnit ast) {
        List<Issue> issues = new ArrayList<>();
        
        // 检查类名
        new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(TypeDeclaration<?> type, Void arg) {
                super.visit(type, arg);
                String className = type.getNameAsString();
                if (!className.isEmpty() && !Character.isUpperCase(className.charAt(0))) {
                    if (!PASCAL_CASE.matcher(className).matches()) {
                        Issue issue = Issue.builder()
                                .severity(Issue.SEVERITY_SUGGESTION)
                                .lineNumber(type.getBegin().map(p -> p.line).orElse(0))
                                .description("类名'" + className + "'应使用帕斯卡命名法（首字母大写）")
                                .suggestion("建议将类名改为符合帕斯卡命名规范的格式")
                                .build();
                        issues.add(issue);
                    }
                }
            }
        }.visit(ast, null);
        
        // 检查方法名
        new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodDeclaration method, Void arg) {
                super.visit(method, arg);
                String methodName = method.getNameAsString();
                if (!CAMEL_CASE.matcher(methodName).matches() && !methodName.equals("main")) {
                    Issue issue = Issue.builder()
                            .severity(Issue.SEVERITY_SUGGESTION)
                            .lineNumber(method.getBegin().map(p -> p.line).orElse(0))
                            .description("方法名'" + methodName + "'应使用驼峰命名法（首字母小写）")
                            .suggestion("建议将方法名改为符合驼峰命名规范的格式")
                            .build();
                    issues.add(issue);
                }
            }
        }.visit(ast, null);
        
        return issues;
    }
    
    /**
     * 检查方法长度
     */
    private List<Issue> checkMethodLength(CompilationUnit ast) {
        List<Issue> issues = new ArrayList<>();
        
        new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodDeclaration method, Void arg) {
                super.visit(method, arg);
                
                int startLine = method.getBegin().map(p -> p.line).orElse(0);
                int endLine = method.getEnd().map(p -> p.line).orElse(startLine);
                int methodLength = endLine - startLine + 1;
                
                if (methodLength > 50) {
                    Issue issue = Issue.builder()
                            .severity(Issue.SEVERITY_WARNING)
                            .lineNumber(startLine)
                            .description("方法'" + method.getNameAsString() + "'行数为" + methodLength + "行，建议不超过50行")
                            .suggestion("考虑将长方法拆分为多个小方法，提高代码可读性和可维护性")
                            .build();
                    issues.add(issue);
                }
            }
        }.visit(ast, null);
        
        return issues;
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
