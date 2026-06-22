package com.codeaudit.engine.scanner;

import com.codeaudit.engine.IssueDraft;
import com.codeaudit.engine.ReviewContext;
import com.codeaudit.engine.executor.RuleExecutor;
import com.codeaudit.entity.Rule;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 敏感信息日志扫描器
 * 规则：SECURITY_SENSITIVE_LOG
 * 模式：
 *   - log.info/debug/warn/error(...) 中直接打印 password / pwd / token / secret / idcard / id_number / 身份证
 *   - System.out.println(...) 同上
 *   - logger.xxx(password) 形式
 */
@Component("sensitiveLogScanner")
public class SensitiveLogScanner implements RuleExecutor {

    /** 触发"含敏感字段"的正则 */
    private static final Pattern SENSITIVE_KEYWORD = Pattern.compile(
            "(?i)\\b(password|passwd|pwd|secret|token|access[_-]?token|api[_-]?key|app[_-]?secret|private[_-]?key|credentials?|authorization|cookie|session[_-]?id|身份证|id[_-]?card|id[_-]?number|credit[_-]?card|card[_-]?number|cvv)\\b"
    );

    /** 抓日志调用 */
    private static final Pattern[] LOG_CALLS = new Pattern[]{
            // log.info("...", password) / logger.debug("pwd=" + password)
            Pattern.compile("(?i)\\b(log|logger)\\.(trace|debug|info|warn|error|fatal)\\s*\\("),
            Pattern.compile("(?i)\\bSystem\\.out\\.print(ln)?\\s*\\("),
    };

    @Override
    public void execute(ReviewContext ctx, Rule rule) {
        if (!"SECURITY_SENSITIVE_LOG".equals(rule.getCode())) return;
        String code = ctx.getPreprocessedCode();
        if (code == null) return;

        for (Pattern p : LOG_CALLS) {
            Matcher m = p.matcher(code);
            while (m.find()) {
                int start = m.start();
                // 找匹配的右括号，确定整条语句
                int end = matchParen(code, m.end() - 1);
                if (end < 0) continue;
                String call = code.substring(start, end + 1);
                Matcher kw = SENSITIVE_KEYWORD.matcher(call);
                if (kw.find()) {
                    int lineNo = lineOf(code, start);
                    String sensitive = kw.group();
                    ctx.getIssues().add(IssueDraft.builder()
                            .ruleId(rule.getId()).ruleName(rule.getName())
                            .category("SECURITY").severity(rule.getSeverity())
                            .lineNumber(lineNo)
                            .description("日志/输出语句中可能打印敏感字段 '" + sensitive + "'，存在敏感信息泄漏风险")
                            .suggestion("在日志中脱敏：log.info(\"user login: {} ***\", mask(token)); 编写 mask() 对中间字符替换为 ***；禁止打印明文密码、Token、身份证、银行卡")
                            .codeBefore(truncate(call, 200))
                            .codeAfter("// 脱敏后再打印\nlog.info(\"user login, token={}***\", token == null ? null : token.substring(0, 4));")
                            .build());
                }
            }
        }
    }

    /** 找与 start 位置 '(' 配对的 ')'，正确处理字符串内的括号 */
    private int matchParen(String code, int openIdx) {
        if (openIdx < 0 || openIdx >= code.length() || code.charAt(openIdx) != '(') return -1;
        int depth = 0;
        boolean inStr = false;
        boolean inChr = false;
        for (int i = openIdx; i < code.length(); i++) {
            char c = code.charAt(i);
            if (inStr) {
                if (c == '\\') { i++; continue; }
                if (c == '"') inStr = false;
            } else if (inChr) {
                if (c == '\\') { i++; continue; }
                if (c == '\'') inChr = false;
            } else {
                if (c == '"') inStr = true;
                else if (c == '\'') inChr = true;
                else if (c == '(') depth++;
                else if (c == ')') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return -1;
    }

    private int lineOf(String code, int offset) {
        int n = 1;
        for (int i = 0; i < offset; i++) if (code.charAt(i) == '\n') n++;
        return n;
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + " ...";
    }

    @Override public String category() { return "SECURITY"; }
    @Override public String patternType() { return "REGEX"; }
}
