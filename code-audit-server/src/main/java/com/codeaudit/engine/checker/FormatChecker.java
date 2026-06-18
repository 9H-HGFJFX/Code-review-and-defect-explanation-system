package com.codeaudit.engine.checker;

import com.codeaudit.engine.IssueDraft;
import com.codeaudit.engine.ReviewContext;
import com.codeaudit.engine.executor.RuleExecutor;
import com.codeaudit.entity.Rule;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.springframework.stereotype.Component;

/**
 * 格式检查器
 * 规则：STYLE_LINE_TOO_LONG
 */
@Component("formatChecker")
public class FormatChecker implements RuleExecutor {

    private static final int MAX_LINE_WIDTH = 120;

    @Override
    public void execute(ReviewContext ctx, Rule rule) {
        if (!"STYLE_LINE_TOO_LONG".equals(rule.getCode())) return;
        String code = ctx.getPreprocessedCode();
        if (code == null) return;
        String[] lines = code.split("\n");
        for (int i = 0; i < lines.length; i++) {
            int len = lines[i].length();
            if (len > MAX_LINE_WIDTH) {
                ctx.getIssues().add(IssueDraft.builder()
                        .ruleId(rule.getId()).ruleName(rule.getName())
                        .category("STYLE").severity(rule.getSeverity())
                        .lineNumber(i + 1)
                        .description("第 " + (i + 1) + " 行字符数 " + len + " 超出建议值 " + MAX_LINE_WIDTH)
                        .suggestion("将长行拆分为多行，提升可读性")
                        .codeBefore(truncate(lines[i], 200))
                        .build());
            }
        }
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + " ...";
    }

    @Override public String category() { return "STYLE"; }
    @Override public String patternType() { return "AST"; }
}
