package com.codeaudit.engine.scanner;

import com.codeaudit.engine.IssueDraft;
import com.codeaudit.engine.ReviewContext;
import com.codeaudit.engine.executor.RuleExecutor;
import com.codeaudit.entity.Rule;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 命令注入扫描器
 * 模式：
 *   Runtime.getRuntime().exec(...含字符串拼接...)
 *   new ProcessBuilder(...含字符串拼接...)
 */
@Component("commandInjectionScanner")
public class CommandInjectionScanner implements RuleExecutor {

    private static final Pattern[] PATTERNS = new Pattern[]{
            Pattern.compile("(?is)Runtime\\.getRuntime\\(\\)\\.exec\\s*\\(\\s*\"[^\"]*\"\\s*\\+\\s*[a-zA-Z_]"),
            Pattern.compile("(?is)new\\s+ProcessBuilder\\s*\\([^)]*\\+\\s*[a-zA-Z_]"),
            Pattern.compile("(?is)new\\s+ProcessBuilder\\s*\\(\\s*[a-zA-Z_]\\s*\\+")
    };

    @Override
    public void execute(ReviewContext ctx, Rule rule) {
        if (!"SECURITY_COMMAND_INJECTION".equals(rule.getCode())) return;
        String code = ctx.getPreprocessedCode();
        if (code == null) return;

        for (Pattern p : PATTERNS) {
            Matcher m = p.matcher(code);
            while (m.find()) {
                int lineNo = 1;
                for (int i = 0; i < m.start(); i++) {
                    if (code.charAt(i) == '\n') lineNo++;
                }
                ctx.getIssues().add(IssueDraft.builder()
                        .ruleId(rule.getId()).ruleName(rule.getName())
                        .category("SECURITY").severity(rule.getSeverity())
                        .lineNumber(lineNo)
                        .description("检测到命令注入风险：用户输入拼接到系统命令执行函数")
                        .suggestion("避免拼接用户输入到命令；使用 ProcessBuilder 加白名单校验；或使用 SecureRandom 等安全 API")
                        .codeBefore(m.group())
                        .codeAfter("// 1) 校验入参白名单\n// 2) 拆分命令和参数\nProcessBuilder pb = new ProcessBuilder(\"ls\", \"-l\", safeDir);")
                        .build());
            }
        }
    }

    @Override public String category() { return "SECURITY"; }
    @Override public String patternType() { return "REGEX"; }
}
