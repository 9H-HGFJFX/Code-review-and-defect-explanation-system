package com.codeaudit.engine.detector;

import com.codeaudit.engine.IssueDraft;
import com.codeaudit.engine.ReviewContext;
import com.codeaudit.engine.executor.RuleExecutor;
import com.codeaudit.entity.Rule;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 死循环检测器
 * 规则：DEFECT_INFINITE_LOOP
 * 检测 while(true)/for(;;) 循环体内是否有 break/return 等终止语句（粗略判断）。
 */
@Component("infiniteLoopDetector")
public class InfiniteLoopDetector implements RuleExecutor {

    @Override
    public void execute(ReviewContext ctx, Rule rule) {
        if (!"DEFECT_INFINITE_LOOP".equals(rule.getCode())) return;
        CompilationUnit cu = ctx.getCompilationUnit();
        if (cu == null) return;

        cu.findAll(WhileStmt.class).forEach(w -> {
            if (isLiteralTrue(w.getCondition())) {
                int line = w.getBegin().map(p -> p.line).orElse(0);
                if (!hasExit(extractStatements(w.getBody()))) {
                    ctx.getIssues().add(IssueDraft.builder()
                            .ruleId(rule.getId()).ruleName(rule.getName())
                            .category("DEFECT").severity(rule.getSeverity())
                            .lineNumber(line)
                            .description("while(true) 循环体内未发现 break/return/throw 等终止语句，可能死循环")
                            .suggestion("在循环体内增加 break / return / throw 等出口；或使用信号量等机制控制循环结束")
                            .codeBefore("while (true) {\n    // ...\n}")
                            .codeAfter("while (running) {\n    // ...\n    if (done) break;\n}")
                            .build());
                }
            }
        });

        cu.findAll(DoStmt.class).forEach(d -> {
            if (isLiteralTrue(d.getCondition())) {
                int line = d.getBegin().map(p -> p.line).orElse(0);
                if (!hasExit(extractStatements(d.getBody()))) {
                    ctx.getIssues().add(IssueDraft.builder()
                            .ruleId(rule.getId()).ruleName(rule.getName())
                            .category("DEFECT").severity(rule.getSeverity())
                            .lineNumber(line)
                            .description("do-while(true) 循环体内未发现 break/return/throw 等终止语句")
                            .suggestion("在循环体内增加显式出口")
                            .codeBefore("do {\n    // ...\n} while (true);")
                            .codeAfter("do {\n    // ...\n    if (done) break;\n} while (running);")
                            .build());
                }
            }
        });

        cu.findAll(ForStmt.class).forEach(f -> {
            if (f.getCompare().isEmpty() && f.getInitialization().isEmpty() && f.getUpdate().isEmpty()) {
                int line = f.getBegin().map(p -> p.line).orElse(0);
                if (!hasExit(extractStatements(f.getBody()))) {
                    ctx.getIssues().add(IssueDraft.builder()
                            .ruleId(rule.getId()).ruleName(rule.getName())
                            .category("DEFECT").severity(rule.getSeverity())
                            .lineNumber(line)
                            .description("for(;;) 循环体内未发现 break/return/throw 等终止语句，可能死循环")
                            .suggestion("在循环体内增加显式出口")
                            .codeBefore("for (;;) {\n    // ...\n}")
                            .codeAfter("for (;;) {\n    // ...\n    if (done) break;\n}")
                            .build());
                }
            }
        });
    }

    private boolean isLiteralTrue(Expression cond) {
        if (cond == null) return false;
        if (cond instanceof BooleanLiteralExpr b) return b.getValue();
        if (cond instanceof UnaryExpr u
                && u.getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT
                && u.getExpression() instanceof BooleanLiteralExpr inner) {
            return !inner.getValue();
        }
        return false;
    }

    private boolean hasExit(List<Statement> stmts) {
        if (stmts == null || stmts.isEmpty()) return false;
        for (Statement s : stmts) {
            if (s instanceof BreakStmt) return true;
            if (s instanceof ReturnStmt) return true;
            // throw 不在 stmt 直接树，JavaParser 把它包在 ThrowStmt
            if (s.toString().startsWith("throw ")) return true;
            // System.exit(...)
            if (s.toString().contains("System.exit")) return true;
            // 嵌套 if 内 break/return 也算
            if (s.isIfStmt()) {
                Statement thenStmt = s.asIfStmt().getThenStmt();
                if (hasExit(extractStatements(thenStmt))) return true;
                Optional<Statement> elseStmt = s.asIfStmt().getElseStmt();
                if (elseStmt.isPresent() && hasExit(extractStatements(elseStmt.get()))) return true;
            }
            // 嵌套 while/for 内部 break 不算当前循环的出口（外层还需有自己出口）
        }
        return false;
    }

    /**
     * 把一个 Statement 拆成可遍历的子语句列表。
     * BlockStmt 取 statements；其他单语句包装为单元素列表。
     */
    private List<Statement> extractStatements(Statement s) {
        if (s == null) return java.util.Collections.emptyList();
        if (s instanceof com.github.javaparser.ast.stmt.BlockStmt b) return b.getStatements();
        return java.util.Collections.singletonList(s);
    }

    @Override public String category() { return "DEFECT"; }
    @Override public String patternType() { return "AST"; }
}
