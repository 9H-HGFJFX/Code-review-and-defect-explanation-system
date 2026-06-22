package com.codeaudit.engine.scanner;

import com.codeaudit.engine.IssueDraft;
import com.codeaudit.engine.ReviewContext;
import com.codeaudit.engine.executor.RuleExecutor;
import com.codeaudit.entity.Rule;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 路径遍历扫描器
 * 规则：SECURITY_PATH_TRAVERSAL
 * 模式：
 *   - new File(request.getParameter(...)) / Paths.get(request.getParameter(...))
 *   - FileOutputStream / FileWriter / FileReader / FileInputStream 直接接收 request 拼接的字符串
 *   - String 路径与 request 拼接后传给文件 API
 */
@Component("pathTraversalScanner")
public class PathTraversalScanner implements RuleExecutor {

    // 抓 request.* 出现在文件 API 上下文的情况
    private static final Pattern[] PATTERNS = new Pattern[]{
            // new File( ... 包含 request.getParameter / request.getHeader 等
            Pattern.compile("(?i)new\\s+File\\s*\\([^)]*request\\.(getParameter|getHeader|getCookie|getPathInfo|getQueryString)\\b[^)]*\\)"),
            // new File(String + 变量)
            Pattern.compile("(?i)new\\s+File\\s*\\([^)]*\\+\\s*\\w+"),
            // Paths.get(... request.*)
            Pattern.compile("(?i)Paths\\.get\\s*\\([^)]*request\\.(getParameter|getHeader|getCookie|getPathInfo|getQueryString)\\b[^)]*\\)"),
            // new FileInputStream/OutputStream/Writer/Reader( ... 字符串拼接)
            Pattern.compile("(?is)new\\s+File(Input|Output)Stream\\s*\\([^)]*\\+\\s*[a-zA-Z_]"),
            Pattern.compile("(?is)new\\s+File(Reader|Writer)\\s*\\([^)]*\\+\\s*[a-zA-Z_]"),
    };

    @Override
    public void execute(ReviewContext ctx, Rule rule) {
        if (!"SECURITY_PATH_TRAVERSAL".equals(rule.getCode())) return;
        String code = ctx.getPreprocessedCode();
        if (code == null) return;

        for (Pattern p : PATTERNS) {
            Matcher m = p.matcher(code);
            while (m.find()) {
                int lineNo = lineOf(code, m.start());
                String snippet = m.group();
                ctx.getIssues().add(IssueDraft.builder()
                        .ruleId(rule.getId()).ruleName(rule.getName())
                        .category("SECURITY").severity(rule.getSeverity())
                        .lineNumber(lineNo)
                        .description("检测到路径遍历风险：未校验的用户输入直接用于文件路径构造 '" + snippet + "'")
                        .suggestion("使用白名单校验或规范化路径：\n  1) 通过 Paths.get(base).resolve(rel).normalize() 解析\n  2) 校验最终路径仍以白名单目录为前缀（startsWith）\n  3) 拒绝包含 '..' 或绝对路径前缀的输入")
                        .codeBefore(snippet)
                        .codeAfter("// 1) 校验：拒绝 '..' 与绝对路径\nif (rel.contains(\"..\") || Paths.get(rel).isAbsolute()) {\n    throw new IllegalArgumentException(\"非法路径\");\n}\n// 2) 规范化后判断仍在白名单目录内\nPath safe = Paths.get(BASE_DIR).resolve(rel).normalize();\nif (!safe.startsWith(BASE_DIR)) throw new SecurityException(\"越界\");\n// 3) 再做文件操作")
                        .build());
            }
        }
    }

    private int lineOf(String code, int offset) {
        int n = 1;
        for (int i = 0; i < offset; i++) if (code.charAt(i) == '\n') n++;
        return n;
    }

    @Override public String category() { return "SECURITY"; }
    @Override public String patternType() { return "REGEX"; }
}
