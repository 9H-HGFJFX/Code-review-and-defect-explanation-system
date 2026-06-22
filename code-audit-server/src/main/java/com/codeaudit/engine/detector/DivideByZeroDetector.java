package com.codeaudit.engine.detector;

import com.codeaudit.engine.IssueDraft;
import com.codeaudit.engine.ReviewContext;
import com.codeaudit.engine.executor.RuleExecutor;
import com.codeaudit.entity.Rule;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 除零风险检测器
 * 规则：DEFECT_DIVIDE_BY_ZERO
 * 检测 a / b、a % b 中 b 是字面量 0 的情况。
 * 变量/方法返回值无法静态判断，仅在编译期可证伪的场景下报告。
 */
@Component("divideByZeroDetector")
public class DivideByZeroDetector implements RuleExecutor {

    @Override
    public void execute(ReviewContext ctx, Rule rule) {
        if (!"DEFECT_DIVIDE_BY_ZERO".equals(rule.getCode())) return;
        CompilationUnit cu = ctx.getCompilationUnit();
        if (cu == null) return;

        cu.findAll(BinaryExpr.class).forEach(be -> {
            if (be.getOperator() != BinaryExpr.Operator.DIVIDE
                    && be.getOperator() != BinaryExpr.Operator.REMAINDER) return;

            if (isLiteralZero(be.getRight())) {
                int line = be.getBegin().map(p -> p.line).orElse(0);
                String op = be.getOperator() == BinaryExpr.Operator.DIVIDE ? "/" : "%";
                String snippet = be.toString();
                ctx.getIssues().add(IssueDraft.builder()
                        .ruleId(rule.getId()).ruleName(rule.getName())
                        .category("DEFECT").severity(rule.getSeverity())
                        .lineNumber(line)
                        .description("除法/取模 '" + snippet + "' 的除数为字面量 0，将抛出 ArithmeticException")
                        .suggestion("在执行前校验除数非 0：if (divisor != 0) { result = a " + op + " divisor; }")
                        .codeBefore(snippet)
                        .codeAfter("if (divisor != 0) {\n    int result = a " + op + " divisor;\n} else {\n    // 错误处理\n}")
                        .build());
            }
        });
    }

    private boolean isLiteralZero(Expression e) {
        if (e == null) return false;
        // 直接 0
        if (e instanceof IntegerLiteralExpr i) {
            return i.asNumber() != null && i.asNumber().intValue() == 0;
        }
        // -0 / +0
        if (e instanceof UnaryExpr u
                && (u.getOperator() == UnaryExpr.Operator.MINUS
                || u.getOperator() == UnaryExpr.Operator.PLUS)
                && u.getExpression() instanceof IntegerLiteralExpr i2) {
            return i2.asNumber() != null && i2.asNumber().intValue() == 0;
        }
        return false;
    }

    @Override public String category() { return "DEFECT"; }
    @Override public String patternType() { return "AST"; }
}
