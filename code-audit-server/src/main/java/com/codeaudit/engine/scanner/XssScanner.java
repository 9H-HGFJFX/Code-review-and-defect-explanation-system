package com.codeaudit.engine.scanner;

import com.codeaudit.engine.IssueDraft;
import com.codeaudit.engine.ReviewContext;
import com.codeaudit.engine.executor.RuleExecutor;
import com.codeaudit.entity.Rule;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * XSS 风险扫描器
 * 模式：
 *   - response.getWriter().write(request.getParameter(...))
 *   - out.print(request.getParameter(...))
 *   - 直接拼接用户输入到 HTML/JS 字符串
 */
@Component("xssScanner")
public class XssScanner implements RuleExecutor {

    private static final Pattern[] PATTERNS = new Pattern[]{
            // response.getWriter().write(...);  / println(...)  / print(...)
            Pattern.compile("(?i)(getWriter|getOutputStream|print|println|write)\\s*\\([^)]*request\\.(getParameter|getHeader|getCookie)\\b[^)]*\\)"),
            // innerHTML = "...${...}..." 直接拼接用户输入
            Pattern.compile("(?i)\\.innerHTML\\s*=\\s*[^;]*(request\\.|document\\.location|window\\.location)"),
            // 写到 .jsp 页面的拼接
            Pattern.compile("(?i)out\\.(print|println)\\s*\\([^)]*(request\\.|session\\.)[^)]*\\)")
    };

    @Override
    public void execute(ReviewContext ctx, Rule rule) {
        if (!"SECURITY_XSS".equals(rule.getCode())) return;
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
                        .description("未转义的用户输入直接输出到页面，存在 XSS 风险")
                        .suggestion("使用 Thymeleaf/Vue 模板的自动转义；或使用 OWASP Java Encoder：Encoder.encodeForHTML(input)")
                        .codeBefore(m.group())
                        .codeAfter("// 使用 OWASP Encoder 编码后输出\nString safe = Encode.forHtml(input);")
                        .build());
            }
        }
    }

    @Override public String category() { return "SECURITY"; }
    @Override public String patternType() { return "REGEX"; }
}
