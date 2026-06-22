package com.codeaudit.engine.scanner;

import com.codeaudit.engine.IssueDraft;
import com.codeaudit.engine.ReviewContext;
import com.codeaudit.engine.executor.RuleExecutor;
import com.codeaudit.entity.Rule;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 反序列化风险扫描器
 * 规则：SECURITY_UNSAFE_DESERIALIZE
 * 模式：
 *   - new ObjectInputStream(...).readObject()
 *   - ObjectInputStream.readUnshared()
 *   - XMLDecoder.readObject()
 *   - Jackson enableDefaultTyping / activateDefaultTyping
 *   - XStream.fromXML(...) 用于不可信输入
 */
@Component("unsafeDeserializeScanner")
public class UnsafeDeserializeScanner implements RuleExecutor {

    private static final Pattern[] PATTERNS = new Pattern[]{
            // new ObjectInputStream(...) 自身即可疑（潜在 readObject 调用）
            Pattern.compile("(?is)new\\s+ObjectInputStream\\s*\\("),
            // 显式 .readObject() / readUnshared() 调用
            Pattern.compile("(?is)\\.\\s*readObject\\s*\\("),
            Pattern.compile("(?is)\\.\\s*readUnshared\\s*\\("),
            Pattern.compile("(?is)new\\s+XMLDecoder\\s*\\([^)]*\\)"),
            Pattern.compile("(?is)\\.\\s*enableDefaultTyping\\s*\\("),
            Pattern.compile("(?is)\\.\\s*activateDefaultTyping\\s*\\("),
            // SnakeYAML 默认构造器 + loadAll/load
            Pattern.compile("(?is)new\\s+Yaml\\s*\\(\\s*\\)\\s*\\.\\s*load(?:All)?\\s*\\("),
    };

    @Override
    public void execute(ReviewContext ctx, Rule rule) {
        if (!"SECURITY_UNSAFE_DESERIALIZE".equals(rule.getCode())) return;
        String code = ctx.getPreprocessedCode();
        if (code == null) return;

        for (Pattern p : PATTERNS) {
            Matcher m = p.matcher(code);
            while (m.find()) {
                int lineNo = lineOf(code, m.start());
                String snippet = m.group();
                String reason;
                String fix;
                if (snippet.toLowerCase().contains("objectinputstream") || snippet.contains("readObject") || snippet.contains("readUnshared")) {
                    reason = "Java 原生反序列化（ObjectInputStream.readObject）会执行类路径中任意类的 readObject / readResolve，存在反序列化 RCE 风险";
                    fix = "避免对不可信输入使用 Java 原生反序列化；如必须使用，使用 ObjectInputFilter（白名单）限制可反序列化类；或换用 JSON / Protobuf 等数据格式";
                } else if (snippet.toLowerCase().contains("xmldecoder")) {
                    reason = "XMLDecoder 内部依赖 JavaBean 反射 + 任意方法调用，对不可信输入直接反序列化会导致 RCE";
                    fix = "禁用 XMLDecoder；改用 JAXB / Jackson / Gson 等更安全的 XML/JSON 库";
                } else if (snippet.contains("enableDefaultTyping") || snippet.contains("activateDefaultTyping")) {
                    reason = "Jackson 的 default typing 会在序列化结果中包含类型信息，反序列化时按类型多态构造对象，对不可信输入存在 gadget chain RCE 风险";
                    fix = "生产环境不要开启 default typing；使用 @JsonTypeInfo 显式声明多态字段，并使用白名单 + @JsonIgnoreProperties(ignoreUnknown = true)";
                } else if (snippet.contains("Yaml")) {
                    reason = "SnakeYAML 默认构造器使用全局 tag，可触发任意类构造；对不可信 YAML 输入存在 RCE 风险";
                    fix = "使用 new Yaml(new SafeConstructor(LoaderOptions.class))；或换用 jackson-dataformat-yaml";
                } else {
                    reason = "检测到不安全的反序列化调用";
                    fix = "对不可信输入避免使用 Java 原生反序列化；优先使用白名单 + 数据格式校验";
                }
                ctx.getIssues().add(IssueDraft.builder()
                        .ruleId(rule.getId()).ruleName(rule.getName())
                        .category("SECURITY").severity(rule.getSeverity())
                        .lineNumber(lineNo)
                        .description(reason)
                        .suggestion(fix)
                        .codeBefore(snippet)
                        .codeAfter("// 1) 优先使用 JSON 等更安全格式\nMyDTO obj = objectMapper.readValue(input, MyDTO.class);\n// 2) 必须用 Java 反序列化时，加 ObjectInputFilter 白名单\nObjectInputFilter filter = ObjectInputFilter.Config.createFilter(\n    \"com.app.dto.*;java.util.*;!*\"\n);")
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
