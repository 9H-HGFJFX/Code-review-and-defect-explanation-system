package com.codeaudit.engine.scanner;

import com.codeaudit.engine.IssueDraft;
import com.codeaudit.engine.ReviewContext;
import com.codeaudit.engine.executor.RuleExecutor;
import com.codeaudit.entity.Rule;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 不安全加密扫描器
 * 规则：SECURITY_INSECURE_CRYPTO
 * 模式：
 *   - 使用 MessageDigest.getInstance("MD5" / "SHA1" / "SHA-1") 做密码哈希
 *   - 使用 Cipher.getInstance("DES" / "DES/ECB/..." / "DESede/ECB") 做对称加密
 *   - 注释中提示 "MD5 / SHA-1" 但不强制
 */
@Component("insecureCryptoScanner")
public class InsecureCryptoScanner implements RuleExecutor {

    private static final Pattern[] PATTERNS = new Pattern[]{
            // MessageDigest.getInstance("MD5"|"SHA-1"|"SHA1")
            Pattern.compile("(?i)MessageDigest\\.getInstance\\s*\\(\\s*\"(MD5|SHA-?1)\"\\s*\\)"),
            // Cipher.getInstance("DES"|"DES/..."|"DESede/ECB")
            Pattern.compile("(?i)Cipher\\.getInstance\\s*\\(\\s*\"(DES(?:ede)?(?:/[A-Za-z0-9]+)*)\"\\s*\\)"),
            // 显式 new SecretKeySpec 长度 8 / 56 位（DES / 弱密钥）
            Pattern.compile("(?is)new\\s+SecretKeySpec\\s*\\([^,]+,\\s*\\s*\"(DES|DESede)\"\\s*\\)"),
    };

    @Override
    public void execute(ReviewContext ctx, Rule rule) {
        if (!"SECURITY_INSECURE_CRYPTO".equals(rule.getCode())) return;
        String code = ctx.getPreprocessedCode();
        if (code == null) return;

        for (Pattern p : PATTERNS) {
            Matcher m = p.matcher(code);
            while (m.find()) {
                int lineNo = lineOf(code, m.start());
                String snippet = m.group();
                String algo = m.groupCount() >= 1 ? m.group(1) : "(未知算法)";
                String reason;
                String fix;
                if (algo.toUpperCase().contains("MD5") || algo.toUpperCase().contains("SHA-1") || algo.toUpperCase().equals("SHA1")) {
                    reason = "MD5 / SHA-1 已被证明存在碰撞攻击，不应用于密码哈希、签名、证书指纹等安全场景";
                    fix = "使用 BCrypt / SCrypt / Argon2 做密码哈希；使用 SHA-256/384/512 做一般摘要；HMAC-SHA256 做完整性校验";
                } else if (algo.toUpperCase().startsWith("DES")) {
                    reason = "DES 密钥长度仅 56 位，可在数小时内暴力破解；ECB 模式会泄露明文结构";
                    fix = "使用 AES（128/256 位）做对称加密；优先选择 GCM 认证加密模式；密钥使用 KeyGenerator 安全生成";
                } else {
                    reason = "检测到不安全的加密算法 " + algo;
                    fix = "使用现代标准算法：AES-GCM / ChaCha20-Poly1305 / SHA-256+HMAC";
                }
                ctx.getIssues().add(IssueDraft.builder()
                        .ruleId(rule.getId()).ruleName(rule.getName())
                        .category("SECURITY").severity(rule.getSeverity())
                        .lineNumber(lineNo)
                        .description(reason + "（" + snippet + "）")
                        .suggestion(fix)
                        .codeBefore(snippet)
                        .codeAfter("// 推荐：使用 BCrypt 处理密码\nString hashed = BCrypt.hashpw(password, BCrypt.gensalt(12));\n// 或 AES-GCM 处理数据加密\nCipher c = Cipher.getInstance(\"AES/GCM/NoPadding\");\nSecretKey key = new SecretKeySpec(secureRandomBytes, \"AES\");")
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
