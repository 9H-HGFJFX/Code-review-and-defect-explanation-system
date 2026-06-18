package com.codeaudit.engine.checker;

import com.codeaudit.engine.IssueDraft;
import com.codeaudit.engine.ReviewContext;
import com.codeaudit.engine.executor.RuleExecutor;
import com.codeaudit.entity.Rule;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.springframework.stereotype.Component;

/**
 * 结构检查器
 * 规则：STYLE_METHOD_TOO_LONG
 */
@Component("structureChecker")
public class StructureChecker implements RuleExecutor {

    private static final int MAX_METHOD_LINES = 80;

    @Override
    public void execute(ReviewContext ctx, Rule rule) {
        if (!"STYLE_METHOD_TOO_LONG".equals(rule.getCode())) return;
        CompilationUnit cu = ctx.getCompilationUnit();
        if (cu == null) return;

        cu.findAll(MethodDeclaration.class).forEach(m -> {
            int start = m.getBegin().map(p -> p.line).orElse(0);
            int end = m.getEnd().map(p -> p.line).orElse(start);
            int len = end - start + 1;
            if (len > MAX_METHOD_LINES) {
                ctx.getIssues().add(IssueDraft.builder()
                        .ruleId(rule.getId()).ruleName(rule.getName())
                        .category("STYLE").severity(rule.getSeverity())
                        .lineNumber(start)
                        .description("方法 '" + m.getNameAsString() + "' 共 " + len + " 行，超过建议值 " + MAX_METHOD_LINES)
                        .suggestion("将方法拆分为多个职责单一的小方法（Extract Method）")
                        .codeBefore(m.getDeclarationAsString() + " { ... }")
                        .build());
            }
        });
    }

    @Override public String category() { return "STYLE"; }
    @Override public String patternType() { return "AST"; }
}
