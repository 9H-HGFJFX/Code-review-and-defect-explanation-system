package com.codeaudit.engine.detector;

import com.codeaudit.engine.IssueDraft;
import com.codeaudit.engine.ReviewContext;
import com.codeaudit.engine.executor.RuleExecutor;
import com.codeaudit.entity.Rule;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.springframework.stereotype.Component;

/**
 * 条件永真/永假检测器
 * 规则：DEFECT_ALWAYS_TRUE / DEFECT_ALWAYS_FALSE
 * 检测 if/while/do-while/for 的条件是字面量 true/false 的情况。
 */
@Component("conditionDetector")
public class ConditionDetector implements RuleExecutor {

    @Override
    public void execute(ReviewContext ctx, Rule rule) {
        if (!"DEFECT_ALWAYS_TRUE".equals(rule.getCode())
                && !"DEFECT_ALWAYS_FALSE".equals(rule.getCode())) {
            return;
        }
        CompilationUnit cu = ctx.getCompilationUnit();
        if (cu == null) return;

        cu.findAll(IfStmt.class).forEach(s -> checkAndReport(ctx, rule, s.getCondition(), s.getBegin().map(p -> p.line).orElse(0), "if"));
        cu.findAll(WhileStmt.class).forEach(s -> checkAndReport(ctx, rule, s.getCondition(), s.getBegin().map(p -> p.line).orElse(0), "while"));
        cu.findAll(DoStmt.class).forEach(s -> checkAndReport(ctx, rule, s.getCondition(), s.getBegin().map(p -> p.line).orElse(0), "do-while"));
        cu.findAll(ForStmt.class).forEach(s -> {
            // for(;;) 条件为 null
            if (s.getCompare().isEmpty()) {
                int line = s.getBegin().map(p -> p.line).orElse(0);
                ctx.getIssues().add(IssueDraft.builder()
                        .ruleId(rule.getId()).ruleName(rule.getName())
                        .category("DEFECT").severity(rule.getSeverity())
                        .lineNumber(line)
                        .description("for 循环缺少循环条件，可能导致死循环")
                        .suggestion("为 for 循环添加终止条件，或改用 while(true) + break 显式控制")
                        .codeBefore("for (;;) { ... }")
                        .codeAfter("for (int i = 0; i < n; i++) { ... }")
                        .build());
            }
        });
    }

    private void checkAndReport(ReviewContext ctx, Rule rule, Expression cond, int line, String kind) {
        if (cond == null) return;
        String desc = null;
        String fix = null;
        String snippet = cond.toString();

        if (cond instanceof BooleanLiteralExpr b) {
            if (b.getValue() && "DEFECT_ALWAYS_TRUE".equals(rule.getCode())) {
                desc = kind + " 条件恒为 true（" + snippet + "），分支体将永远执行";
                fix = "将永真条件改为有效判断；若确需无条件循环，使用 while(true) 并确保内部有 break";
            } else if (!b.getValue() && "DEFECT_ALWAYS_FALSE".equals(rule.getCode())) {
                desc = kind + " 条件恒为 false（" + snippet + "），分支体永远不会执行";
                fix = "移除死分支；或将条件修正为有效判断";
            }
        } else if (cond instanceof UnaryExpr u
                && u.getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT
                && u.getExpression() instanceof BooleanLiteralExpr inner) {
            if (!inner.getValue() && "DEFECT_ALWAYS_TRUE".equals(rule.getCode())) {
                desc = kind + " 条件恒为 true（!false），分支体将永远执行";
                fix = "将永真条件改为有效判断";
            } else if (inner.getValue() && "DEFECT_ALWAYS_FALSE".equals(rule.getCode())) {
                desc = kind + " 条件恒为 false（!true），分支体永远不会执行";
                fix = "移除死分支";
            }
        } else if (cond instanceof BinaryExpr be) {
            // x == x, x != x → 永真/永假
            if (be.getOperator() == BinaryExpr.Operator.EQUALS
                    || be.getOperator() == BinaryExpr.Operator.NOT_EQUALS) {
                String left = be.getLeft().toString();
                String right = be.getRight().toString();
                if (left.equals(right)) {
                    if (be.getOperator() == BinaryExpr.Operator.EQUALS
                            && "DEFECT_ALWAYS_TRUE".equals(rule.getCode())) {
                        desc = kind + " 条件 '" + snippet + "' 中操作数相同，结果恒为 true";
                        fix = "比较两侧是否真的需要比较；若想判断非空，改用 '" + left + " != null'";
                    } else if (be.getOperator() == BinaryExpr.Operator.NOT_EQUALS
                            && "DEFECT_ALWAYS_FALSE".equals(rule.getCode())) {
                        desc = kind + " 条件 '" + snippet + "' 中操作数相同，结果恒为 false";
                        fix = "比较两侧是否真的需要比较；若想判断非空，改用 '" + left + " != null'";
                    }
                }
            }
        }

        if (desc != null) {
            ctx.getIssues().add(IssueDraft.builder()
                    .ruleId(rule.getId()).ruleName(rule.getName())
                    .category("DEFECT").severity(rule.getSeverity())
                    .lineNumber(line)
                    .description(desc)
                    .suggestion(fix)
                    .codeBefore(kind + " (" + snippet + ") { ... }")
                    .codeAfter("// 修正后的有效条件\n" + kind + " (/* 实际条件 */) { ... }")
                    .build());
        }
    }

    @Override public String category() { return "DEFECT"; }
    @Override public String patternType() { return "AST"; }
}
