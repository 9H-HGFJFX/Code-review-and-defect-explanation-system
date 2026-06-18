package com.codeaudit.engine.detector;

import com.codeaudit.engine.IssueDraft;
import com.codeaudit.engine.ReviewContext;
import com.codeaudit.engine.executor.RuleExecutor;
import com.codeaudit.entity.Rule;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.TryStmt;
import org.springframework.stereotype.Component;

@Component("emptyCatchDetector")
public class EmptyCatchDetector implements RuleExecutor {

    @Override
    public void execute(ReviewContext ctx, Rule rule) {
        if (!"DEFECT_EMPTY_CATCH".equals(rule.getCode())) return;
        CompilationUnit cu = ctx.getCompilationUnit();
        if (cu == null) return;

        for (TryStmt t : cu.findAll(TryStmt.class)) {
            for (CatchClause cc : t.getCatchClauses()) {
                if (cc.getBody().getStatements().isEmpty()) {
                    int line = cc.getBegin().map(p -> p.line).orElse(0);
                    ctx.getIssues().add(IssueDraft.builder()
                            .ruleId(rule.getId()).ruleName(rule.getName())
                            .category("DEFECT").severity(rule.getSeverity())
                            .lineNumber(line)
                            .description("空的 catch 块会吞掉异常，不利于排查问题")
                            .suggestion("至少记录日志：log.error(\"xxx failed\", e);")
                            .codeBefore("} catch (Exception e) {\n}")
                            .codeAfter("} catch (Exception e) {\n    log.error(\"xxx failed\", e);\n}")
                            .build());
                }
            }
        }
    }

    @Override public String category() { return "DEFECT"; }
    @Override public String patternType() { return "AST"; }
}
