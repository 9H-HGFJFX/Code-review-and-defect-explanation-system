package com.codeaudit.engine.scanner;

import com.codeaudit.engine.IssueDraft;
import com.codeaudit.engine.ReviewContext;
import com.codeaudit.engine.executor.RuleExecutor;
import com.codeaudit.entity.Rule;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 注入扫描器 - 字符串拼接构造 SQL
 * 模式：
 *   - 出现 "..." + xxx + "..." 形式，且包含 SELECT/INSERT/UPDATE/DELETE 关键字
 */
@Component("sqlInjectionScanner")
public class SqlInjectionScanner implements RuleExecutor {

    private static final Pattern SQL_STRING_CONCAT = Pattern.compile(
            "(?i)(\"(?:SELECT|INSERT|UPDATE|DELETE)[^\"]*\"\\s*\\+\\s*\\w+|\\w+\\s*\\+\\s*\"(?:SELECT|INSERT|UPDATE|DELETE)[^\"]*\")"
    );

    @Override
    public void execute(ReviewContext ctx, Rule rule) {
        if (!"SECURITY_SQL_INJECTION".equals(rule.getCode())) return;
        String code = ctx.getPreprocessedCode();
        if (code == null) return;

        Matcher m = SQL_STRING_CONCAT.matcher(code);
        int lineNo = 0;
        int lastIdx = 0;
        while (m.find()) {
            // 计算行号
            lineNo = 1;
            for (int i = 0; i < m.start(); i++) {
                if (code.charAt(i) == '\n') lineNo++;
            }
            String snippet = m.group();
            ctx.getIssues().add(IssueDraft.builder()
                    .ruleId(rule.getId()).ruleName(rule.getName())
                    .category("SECURITY").severity(rule.getSeverity())
                    .lineNumber(lineNo)
                    .description("检测到 SQL 注入风险：使用字符串拼接构造 SQL 语句")
                    .suggestion("使用 PreparedStatement 参数化查询：String sql = \"SELECT * FROM users WHERE id = ?\"; ps.setLong(1, id);")
                    .codeBefore(snippet.length() > 200 ? snippet.substring(0, 200) + "..." : snippet)
                    .codeAfter("String sql = \"SELECT * FROM users WHERE id = ?\";\nPreparedStatement ps = conn.prepareStatement(sql);\nps.setLong(1, id);")
                    .build());
        }
    }

    @Override public String category() { return "SECURITY"; }
    @Override public String patternType() { return "REGEX"; }
}
