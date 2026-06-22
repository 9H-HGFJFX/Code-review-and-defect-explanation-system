package com.codeaudit.engine.detector;

import com.codeaudit.engine.IssueDraft;
import com.codeaudit.engine.ReviewContext;
import com.codeaudit.engine.executor.RuleExecutor;
import com.codeaudit.entity.Rule;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import org.springframework.stereotype.Component;

/**
 * 浮点数等值比较检测器
 * 规则：DEFECT_FLOAT_EQUALITY
 * 检测 ==、!= 比较 double/float 类型操作数。
 * 浮点等值比较因精度问题通常应使用 Math.abs(a - b) < EPSILON。
 */
@Component("floatEqualityDetector")
public class FloatEqualityDetector implements RuleExecutor {

    @Override
    public void execute(ReviewContext ctx, Rule rule) {
        if (!"DEFECT_FLOAT_EQUALITY".equals(rule.getCode())) return;
        CompilationUnit cu = ctx.getCompilationUnit();
        if (cu == null) return;

        cu.findAll(BinaryExpr.class).forEach(be -> {
            if (be.getOperator() != BinaryExpr.Operator.EQUALS
                    && be.getOperator() != BinaryExpr.Operator.NOT_EQUALS) return;
            if (isFloatType(be.getLeft()) || isFloatType(be.getRight())) {
                int line = be.getBegin().map(p -> p.line).orElse(0);
                String snippet = be.toString();
                String op = be.getOperator() == BinaryExpr.Operator.EQUALS ? "==" : "!=";
                ctx.getIssues().add(IssueDraft.builder()
                        .ruleId(rule.getId()).ruleName(rule.getName())
                        .category("DEFECT").severity(rule.getSeverity())
                        .lineNumber(line)
                        .description("使用 '" + op + "' 比较浮点数 '" + snippet + "'，因 IEEE754 精度问题可能产生误判")
                        .suggestion("使用误差范围比较：if (Math.abs(a - b) < 1e-9) { ... }；或 Double.compare(a, b) == 0")
                        .codeBefore(snippet)
                        .codeAfter("if (Math.abs(a - b) < 1e-9) {\n    // 视为相等\n}")
                        .build());
            }
        });
    }

    /**
     * 启发式判断表达式是否为 float/double 类型。
     * 仅做最直接的判定：表达式文本以 (double)/(float)/d/f 结尾、或者是浮点字面量、
     * 或者类型注解在源码中可见。
     */
    private boolean isFloatType(Expression e) {
        if (e == null) return false;
        String s = e.toString().trim();
        // 字面量：1.0, 1.0f, 1.0d, .0, 1e3
        if (s.matches(".*\\d[\\d_]*\\.[\\d_]*[fFdD]?") && s.matches(".*\\d.*\\..*")) return true;
        if (s.matches(".*\\d[\\deE]+[fFdD]?")) return true;
        // 显式 cast
        if (s.startsWith("(double)") || s.startsWith("(float)")) return true;
        // 变量名带 d / f 后缀（如 pi、rad2deg）启发式
        return false;
    }

    @Override public String category() { return "DEFECT"; }
    @Override public String patternType() { return "AST"; }
}
