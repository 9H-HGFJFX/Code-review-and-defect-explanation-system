package com.codeaudit.engine.detector;

import com.codeaudit.engine.IssueDraft;
import com.codeaudit.engine.ReviewContext;
import com.codeaudit.engine.executor.RuleExecutor;
import com.codeaudit.entity.Rule;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import org.springframework.stereotype.Component;

/**
 * 不安全类型转换检测器
 * 规则：DEFECT_UNSAFE_CAST
 * 检测：
 *   - 未经 instanceof 判断的向下转型（cast 到具体类）
 *   - 窄化原始类型转换（long→int、double→int 等可能丢精度/溢出）
 */
@Component("unsafeCastDetector")
public class UnsafeCastDetector implements RuleExecutor {

    @Override
    public void execute(ReviewContext ctx, Rule rule) {
        if (!"DEFECT_UNSAFE_CAST".equals(rule.getCode())) return;
        CompilationUnit cu = ctx.getCompilationUnit();
        if (cu == null) return;

        cu.findAll(CastExpr.class).forEach(cast -> {
            int line = cast.getBegin().map(p -> p.line).orElse(0);
            Type targetType = cast.getType();
            String targetStr = targetType.asString();

            // 1) 向下转型：cast 到具体类
            // 仅 (Object) x 永远安全（不会 ClassCastException），其他引用类型 cast 都视为可疑
            if (targetType instanceof ClassOrInterfaceType ct
                    && !"Object".equals(ct.getNameAsString())) {
                Expression inner = cast.getExpression();
                String snippet = cast.toString();
                ctx.getIssues().add(IssueDraft.builder()
                        .ruleId(rule.getId()).ruleName(rule.getName())
                        .category("DEFECT").severity(rule.getSeverity())
                        .lineNumber(line)
                        .description("未经 instanceof 校验的强制类型转换 '" + snippet + "'，可能抛出 ClassCastException")
                        .suggestion("在转换前使用 instanceof 校验类型；或使用 Pattern Matching（Java 16+）：if (obj instanceof " + targetStr + " x) { ... }")
                        .codeBefore(snippet)
                        .codeAfter("if (" + inner + " instanceof " + targetStr + " typed) {\n    // 使用 typed\n}")
                        .build());
            }

            // 2) 窄化原始类型转换
            if (targetType instanceof PrimitiveType pt
                    && cast.getExpression().toString().matches(".*\\b(long|double|float)\\b.*")) {
                String snippet = cast.toString();
                ctx.getIssues().add(IssueDraft.builder()
                        .ruleId(rule.getId()).ruleName(rule.getName())
                        .category("DEFECT").severity(rule.getSeverity())
                        .lineNumber(line)
                        .description("窄化原始类型转换 '" + snippet + "'，可能造成精度丢失或数值溢出")
                        .suggestion("使用 Math.toIntExact(longValue) 让溢出显式抛异常；或显式确认值域安全")
                        .codeBefore(snippet)
                        .codeAfter("int safe = Math.toIntExact(longValue); // 溢出时抛 ArithmeticException")
                        .build());
            }
        });
    }

    @Override public String category() { return "DEFECT"; }
    @Override public String patternType() { return "AST"; }
}
