package com.codeaudit.engine.scanner;

import com.codeaudit.engine.IssueDraft;
import com.codeaudit.engine.ReviewContext;
import com.codeaudit.engine.executor.RuleExecutor;
import com.codeaudit.entity.Rule;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 硬编码敏感信息扫描器
 * 模式：
 *   password = "xxx" / apiKey = "xxx" / token = "xxx" / secret = "xxx"
 *   private static final String XXX_KEY = "..."
 */
@Component("hardcodedSecretScanner")
public class HardcodedSecretScanner implements RuleExecutor {

    // 形如: password\s*=\s*"非空"
    private static final Pattern SECRET_ASSIGN = Pattern.compile(
            "(?i)\\b(password|passwd|pwd|api[_-]?key|secret|token|access[_-]?token|app[_-]?secret)\\s*=\\s*\"([^\"]{3,})\""
    );
    // 形如: private static final String XXX_KEY = "value"  且 value 不是 "xxx" / placeholder
    private static final Pattern CONST_SECRET = Pattern.compile(
            "(?i)\\b(?:private|public|protected)?\\s*static\\s+final\\s+String\\s+[A-Z0-9_]*(?:KEY|SECRET|TOKEN|PASSWORD|PWD)\\s*=\\s*\"([^\"]{4,})\""
    );

    private static final Pattern PLACEHOLDER = Pattern.compile(
            "(?i)^(\\$\\{.*\\}|\\*\\*\\*+|xxxx+|your[_-].+|placeholder|change[_-]?me|xxx)$"
    );

    @Override
    public void execute(ReviewContext ctx, Rule rule) {
        if (!"SECURITY_HARDCODED_SECRET".equals(rule.getCode())) return;
        String code = ctx.getPreprocessedCode();
        if (code == null) return;

        scanAndReport(ctx, rule, code, SECRET_ASSIGN);
        scanAndReport(ctx, rule, code, CONST_SECRET);
    }

    private void scanAndReport(ReviewContext ctx, Rule rule, String code, Pattern p) {
        Matcher m = p.matcher(code);
        while (m.find()) {
            String secret = m.groupCount() >= 2 ? m.group(2) : m.group(1);
            if (secret == null || PLACEHOLDER.matcher(secret).matches()) continue;
            int lineNo = 1;
            for (int i = 0; i < m.start(); i++) {
                if (code.charAt(i) == '\n') lineNo++;
            }
            String snippet = m.group();
            ctx.getIssues().add(IssueDraft.builder()
                    .ruleId(rule.getId()).ruleName(rule.getName())
                    .category("SECURITY").severity(rule.getSeverity())
                    .lineNumber(lineNo)
                    .description("检测到硬编码敏感信息（密码/密钥/Token）")
                    .suggestion("将敏感信息移至配置文件或环境变量，并通过配置中心统一管理；生产配置需加密存储")
                    .codeBefore(snippet.length() > 200 ? snippet.substring(0, 200) + "..." : snippet)
                    .codeAfter("@Value(\"${app.secret}\")\nprivate String secret;")
                    .build());
        }
    }

    @Override public String category() { return "SECURITY"; }
    @Override public String patternType() { return "REGEX"; }
}
