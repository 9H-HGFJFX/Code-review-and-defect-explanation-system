package com.codeaudit.engine.detector;

import com.codeaudit.engine.IssueDraft;
import com.codeaudit.engine.ReviewContext;
import com.codeaudit.engine.executor.RuleExecutor;
import com.codeaudit.entity.Rule;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.springframework.stereotype.Component;

@Component("broadCatchDetector")
public class BroadCatchDetector implements RuleExecutor {

    @Override
    public void execute(ReviewContext ctx, Rule rule) {
        if (!"DEFECT_BROAD_CATCH".equals(rule.getCode())) return;
        CompilationUnit cu = ctx.getCompilationUnit();
        if (cu == null) return;

        for (TryStmt t : cu.findAll(TryStmt.class)) {
            for (CatchClause cc : t.getCatchClauses()) {
                cc.getParameter().getType().ifClassOrInterfaceType(type -> {
                    String name = type.getNameAsString();
                    if ("Exception".equals(name) || "Throwable".equals(name)) {
                        int line = cc.getBegin().map(p -> p.line).orElse(0);
                        ctx.getIssues().add(IssueDraft.builder()
                                .ruleId(rule.getId()).ruleName(rule.getName())
                                .category("DEFECT").severity(rule.getSeverity())
                                .lineNumber(line)
                                .description("捕获过宽的异常类型 " + name + "，无法精确处理不同错误")
                                .suggestion("捕获具体异常类型，如 IOException、SQLException、NullPointerException")
                                .codeBefore("} catch (" + name + " e) { ... }")
                                .codeAfter("} catch (IOException e) { ... }")
                                .build());
                    }
                });
            }
        }
    }

    @Override public String category() { return "DEFECT"; }
    @Override public String patternType() { return "AST"; }
}
