package com.codeaudit.engine.checker;

import com.codeaudit.engine.IssueDraft;
import com.codeaudit.engine.ReviewContext;
import com.codeaudit.engine.executor.RuleExecutor;
import com.codeaudit.entity.Rule;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 命名规范检查器
 * 规则：NAMING_CLASS_UPPERCAMEL / NAMING_METHOD_LOWERCAMEL / NAMING_CONSTANT_UPPER_SNAKE
 */
@Component("namingChecker")
public class NamingChecker implements RuleExecutor {

    private static final Pattern CLASS_UPPER = Pattern.compile("^[A-Z][a-zA-Z0-9]*$");
    private static final Pattern METHOD_LOWER = Pattern.compile("^[a-z][a-zA-Z0-9]*$");
    private static final Pattern CONSTANT_UPPER = Pattern.compile("^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$");

    @Override
    public void execute(ReviewContext ctx, Rule rule) {
        CompilationUnit cu = ctx.getCompilationUnit();
        if (cu == null) return;

        switch (rule.getCode()) {
            case "NAMING_CLASS_UPPERCAMEL" -> checkClass(cu, rule, ctx);
            case "NAMING_METHOD_LOWERCAMEL" -> checkMethod(cu, rule, ctx);
            case "NAMING_CONSTANT_UPPER_SNAKE" -> checkConstant(cu, rule, ctx);
            default -> {}
        }
    }

    private void checkClass(CompilationUnit cu, Rule rule, ReviewContext ctx) {
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(c -> {
            String name = c.getNameAsString();
            // 跳过内部匿名类
            if (name.isEmpty()) return;
            if (!CLASS_UPPER.matcher(name).matches()) {
                addIssue(ctx, rule, c.getBegin().map(p -> p.line).orElse(0),
                        "类名 '" + name + "' 不符合大驼峰命名（UpperCamelCase）",
                        c.toString());
            }
        });
    }

    private void checkMethod(CompilationUnit cu, Rule rule, ReviewContext ctx) {
        cu.findAll(MethodDeclaration.class).forEach(m -> {
            String name = m.getNameAsString();
            if (name.isEmpty()) return;
            if (!METHOD_LOWER.matcher(name).matches()) {
                addIssue(ctx, rule, m.getBegin().map(p -> p.line).orElse(0),
                        "方法名 '" + name + "' 不符合小驼峰命名（lowerCamelCase）",
                        m.getDeclarationAsString());
            }
        });
    }

    private void checkConstant(CompilationUnit cu, Rule rule, ReviewContext ctx) {
        // 1) 类字段中 static final 修饰的视为常量
        cu.findAll(FieldDeclaration.class).forEach(f -> {
            if (f.isStatic() && f.isFinal()) {
                for (VariableDeclarator v : f.getVariables()) {
                    String name = v.getNameAsString();
                    if (!CONSTANT_UPPER.matcher(name).matches()) {
                        addIssue(ctx, rule, f.getBegin().map(p -> p.line).orElse(0),
                                "常量名 '" + name + "' 应全大写+下划线分隔（UPPER_SNAKE_CASE）",
                                f.toString());
                    }
                }
            }
        });
        // 2) 局部变量被 final 修饰且全大写时按常量校验
        cu.findAll(VariableDeclarationExpr.class).forEach(vd -> {
            if (vd.isFinal()) {
                for (VariableDeclarator v : vd.getVariables()) {
                    String init = v.getInitializer().map(Object::toString).orElse("");
                    if (init.matches("^\".*\"$|^-?\\d+[LlFfDd]?$")) {
                        String name = v.getNameAsString();
                        if (!name.equals(name.toUpperCase())) {
                            addIssue(ctx, rule, vd.getBegin().map(p -> p.line).orElse(0),
                                    "字面量常量 '" + name + "' 应全大写+下划线分隔", vd.toString());
                        }
                    }
                }
            }
        });
    }

    private void addIssue(ReviewContext ctx, Rule rule, int line, String desc, String snippet) {
        ctx.getIssues().add(IssueDraft.builder()
                .ruleId(rule.getId()).ruleName(rule.getName())
                .category(rule.getCategory()).severity(rule.getSeverity())
                .lineNumber(line).description(desc)
                .suggestion(rule.getSuggestionTemplate())
                .codeBefore(snippet)
                .build());
    }

    @Override public String category() { return "STYLE"; }
    @Override public String patternType() { return "AST"; }
}
