package com.codeaudit.tools;

import com.codeaudit.engine.IssueDraft;
import com.codeaudit.engine.ReviewContext;
import com.codeaudit.engine.ReviewEngine;
import com.codeaudit.engine.checker.FormatChecker;
import com.codeaudit.engine.checker.NamingChecker;
import com.codeaudit.engine.checker.StructureChecker;
import com.codeaudit.engine.detector.BroadCatchDetector;
import com.codeaudit.engine.detector.ConditionDetector;
import com.codeaudit.engine.detector.ConcurrentModDetector;
import com.codeaudit.engine.detector.DivideByZeroDetector;
import com.codeaudit.engine.detector.EmptyCatchDetector;
import com.codeaudit.engine.detector.FloatEqualityDetector;
import com.codeaudit.engine.detector.InfiniteLoopDetector;
import com.codeaudit.engine.detector.NullPointerDetector;
import com.codeaudit.engine.detector.ResourceLeakDetector;
import com.codeaudit.engine.detector.UnsafeCastDetector;
import com.codeaudit.engine.executor.RuleExecutor;
import com.codeaudit.engine.parser.JavaCodeParser;
import com.codeaudit.engine.scanner.CommandInjectionScanner;
import com.codeaudit.engine.scanner.HardcodedSecretScanner;
import com.codeaudit.engine.scanner.InsecureCryptoScanner;
import com.codeaudit.engine.scanner.PathTraversalScanner;
import com.codeaudit.engine.scanner.SensitiveLogScanner;
import com.codeaudit.engine.scanner.SqlInjectionScanner;
import com.codeaudit.engine.scanner.UnsafeDeserializeScanner;
import com.codeaudit.engine.scanner.XssScanner;
import com.codeaudit.entity.Rule;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 引擎冒烟测试 —— 不依赖 Spring/MySQL/Redis，直接 new 所有执行器跑脏代码。
 * 验证 22 条内置规则是否能命中。
 *
 * 运行：mvn -Dtest=EngineSmokeTest test
 */
public class EngineSmokeTest {

    @Test
    public void testAllRulesHit() throws Exception {
        // 1) 准备 22 条规则
        List<Rule> rules = buildRules();

        // 2) new 一个 ReviewEngine 并注入所有执行器
        ReviewEngine engine = newEngineWithExecutors();

        // 3) 准备一段包含各种问题的脏代码
        String dirtyCode = DIRTY_CODE;

        long start = System.currentTimeMillis();
        ReviewContext ctx = engine.review(dirtyCode, "SmokeTest.java", rules);
        long cost = System.currentTimeMillis() - start;

        // 4) 统计
        Map<String, Integer> byRule = new HashMap<>();
        Map<String, Integer> byCat = new HashMap<>();
        Map<String, Integer> bySev = new HashMap<>();
        for (IssueDraft d : ctx.getIssues()) {
            byRule.merge(d.getRuleName(), 1, Integer::sum);
            byCat.merge(d.getCategory(), 1, Integer::sum);
            bySev.merge(d.getSeverity(), 1, Integer::sum);
        }

        System.out.println("\n========== 引擎冒烟测试结果 ==========");
        System.out.println("解析成功: " + (ctx.getCompilationUnit() != null));
        System.out.println("发现问题总数: " + ctx.getIssues().size());
        System.out.println("耗时(ms):     " + cost);
        System.out.println("按分类: " + byCat);
        System.out.println("按级别: " + bySev);
        System.out.println("按规则: " + byRule);
        System.out.println("---------- 问题详情 ----------");
        int i = 1;
        for (IssueDraft d : ctx.getIssues()) {
            System.out.printf("  [%2d] L%-3d %-9s %-7s %s%n",
                    i++, d.getLineNumber() == null ? 0 : d.getLineNumber(),
                    d.getSeverity(), d.getCategory(), d.getRuleName());
            System.out.println("        " + d.getDescription());
        }
        System.out.println("====================================\n");

        // 5) 关键规则命中检查
        assertHit("空指针风险", byRule, "空指针风险");
        assertHit("资源未关闭", byRule, "资源未关闭");
        assertHit("空 catch 块", byRule, "空 catch 块");
        assertHit("捕获过宽异常", byRule, "捕获过宽异常");
        assertHit("条件恒为真", byRule, "条件恒为真");
        assertHit("条件恒为假", byRule, "条件恒为假");
        assertHit("潜在死循环", byRule, "潜在死循环");
        assertHit("不安全类型转换", byRule, "不安全类型转换");
        assertHit("遍历时修改集合", byRule, "遍历时修改集合");
        assertHit("除零风险", byRule, "除零风险");
        assertHit("浮点等值比较", byRule, "浮点等值比较");
        assertHit("SQL 注入风险", byRule, "SQL 注入风险");
        assertHit("硬编码密码", byRule, "硬编码密码");
        assertHit("XSS 风险", byRule, "XSS 风险");
        assertHit("命令注入", byRule, "命令注入");
        assertHit("路径遍历", byRule, "路径遍历");
        assertHit("敏感信息日志", byRule, "敏感信息日志");
        assertHit("不安全加密", byRule, "不安全加密");
        assertHit("反序列化风险", byRule, "反序列化风险");
    }

    private void assertHit(String name, Map<String, Integer> byRule, String ruleName) {
        boolean hit = byRule.containsKey(ruleName) && byRule.get(ruleName) > 0;
        System.out.printf("  %s  %s%n", hit ? "✅" : "❌", name + (hit ? "" : " (未命中!)"));
        assertTrue(hit, "规则未命中: " + name);
    }

    /**
     * 用反射把 RuleExecutor 注入到 ReviewEngine。
     */
    private ReviewEngine newEngineWithExecutors() throws Exception {
        JavaCodeParser parser = new JavaCodeParser();
        ReviewEngine engine = new ReviewEngine(parser, null);

        List<RuleExecutor> executors = Arrays.asList(
                new NamingChecker(),
                new FormatChecker(),
                new StructureChecker(),
                new NullPointerDetector(),
                new ResourceLeakDetector(),
                new EmptyCatchDetector(),
                new BroadCatchDetector(),
                new ConditionDetector(),
                new InfiniteLoopDetector(),
                new UnsafeCastDetector(),
                new ConcurrentModDetector(),
                new DivideByZeroDetector(),
                new FloatEqualityDetector(),
                new SqlInjectionScanner(),
                new HardcodedSecretScanner(),
                new XssScanner(),
                new CommandInjectionScanner(),
                new PathTraversalScanner(),
                new SensitiveLogScanner(),
                new InsecureCryptoScanner(),
                new UnsafeDeserializeScanner()
        );
        Field f = ReviewEngine.class.getDeclaredField("executors");
        f.setAccessible(true);
        f.set(engine, executors);
        Method m = ReviewEngine.class.getDeclaredMethod("init");
        m.setAccessible(true);
        m.invoke(engine);
        return engine;
    }

    private List<Rule> buildRules() {
        String[][] rows = {
                {"类名大驼峰",        "NAMING_CLASS_UPPERCAMEL",      "STYLE",    "AST",   "WARNING",     "namingChecker"},
                {"方法名小驼峰",      "NAMING_METHOD_LOWERCAMEL",     "STYLE",    "AST",   "WARNING",     "namingChecker"},
                {"常量全大写",        "NAMING_CONSTANT_UPPER_SNAKE",  "STYLE",    "AST",   "WARNING",     "namingChecker"},
                {"单行字符超长",      "STYLE_LINE_TOO_LONG",          "STYLE",    "AST",   "SUGGESTION",  "formatChecker"},
                {"方法过长",          "STYLE_METHOD_TOO_LONG",        "STYLE",    "AST",   "WARNING",     "structureChecker"},

                {"空指针风险",        "DEFECT_NULL_POINTER",          "DEFECT",   "AST",   "ERROR",       "nullPointerDetector"},
                {"资源未关闭",        "DEFECT_RESOURCE_LEAK",         "DEFECT",   "AST",   "ERROR",       "resourceLeakDetector"},
                {"空 catch 块",       "DEFECT_EMPTY_CATCH",           "DEFECT",   "AST",   "WARNING",     "emptyCatchDetector"},
                {"捕获过宽异常",      "DEFECT_BROAD_CATCH",           "DEFECT",   "AST",   "WARNING",     "broadCatchDetector"},
                {"条件恒为真",        "DEFECT_ALWAYS_TRUE",           "DEFECT",   "AST",   "ERROR",       "conditionDetector"},
                {"条件恒为假",        "DEFECT_ALWAYS_FALSE",          "DEFECT",   "AST",   "WARNING",     "conditionDetector"},
                {"潜在死循环",        "DEFECT_INFINITE_LOOP",         "DEFECT",   "AST",   "ERROR",       "infiniteLoopDetector"},
                {"不安全类型转换",    "DEFECT_UNSAFE_CAST",           "DEFECT",   "AST",   "WARNING",     "unsafeCastDetector"},
                {"遍历时修改集合",    "DEFECT_CONCURRENT_MODIFICATION","DEFECT",  "AST",   "ERROR",       "concurrentModDetector"},
                {"除零风险",          "DEFECT_DIVIDE_BY_ZERO",        "DEFECT",   "AST",   "ERROR",       "divideByZeroDetector"},
                {"浮点等值比较",      "DEFECT_FLOAT_EQUALITY",        "DEFECT",   "AST",   "WARNING",     "floatEqualityDetector"},

                {"SQL 注入风险",      "SECURITY_SQL_INJECTION",       "SECURITY", "REGEX", "CRITICAL",    "sqlInjectionScanner"},
                {"硬编码密码",        "SECURITY_HARDCODED_SECRET",    "SECURITY", "REGEX", "CRITICAL",    "hardcodedSecretScanner"},
                {"XSS 风险",          "SECURITY_XSS",                 "SECURITY", "REGEX", "CRITICAL",    "xssScanner"},
                {"命令注入",          "SECURITY_COMMAND_INJECTION",   "SECURITY", "REGEX", "CRITICAL",    "commandInjectionScanner"},
                {"路径遍历",          "SECURITY_PATH_TRAVERSAL",      "SECURITY", "REGEX", "CRITICAL",    "pathTraversalScanner"},
                {"敏感信息日志",      "SECURITY_SENSITIVE_LOG",       "SECURITY", "REGEX", "WARNING",     "sensitiveLogScanner"},
                {"不安全加密",        "SECURITY_INSECURE_CRYPTO",     "SECURITY", "REGEX", "CRITICAL",    "insecureCryptoScanner"},
                {"反序列化风险",      "SECURITY_UNSAFE_DESERIALIZE",  "SECURITY", "REGEX", "CRITICAL",    "unsafeDeserializeScanner"},
        };
        List<Rule> out = new ArrayList<>();
        long id = 1;
        for (String[] r : rows) {
            Rule rule = new Rule();
            rule.setId(id++);
            rule.setName(r[0]);
            rule.setCode(r[1]);
            rule.setCategory(r[2]);
            rule.setPatternType(r[3]);
            rule.setSeverity(r[4]);
            rule.setExecutorBean(r[5]);
            rule.setDescription(r[0]);
            rule.setSuggestionTemplate("请参考" + r[0]);
            rule.setEnabled(1);
            out.add(rule);
        }
        return out;
    }

    private static final String DIRTY_CODE =
            "import java.io.*;\n" +
            "import java.sql.*;\n" +
            "import java.security.*;\n" +
            "import javax.crypto.*;\n" +
            "import java.util.*;\n" +
            "import java.util.logging.*;\n" +
            "\n" +
            "public class demoService {\n" +
            "    private static final String api_key = \"ABC123XYZ\";\n" +
            "    private String userInput;\n" +
            "\n" +
            "    public User findUser_bad(Connection conn, String name) {\n" +
            "        String sql = \"SELECT * FROM users WHERE name='\" + name + \"'\";\n" +
            "        FileInputStream fis = new FileInputStream(\"data/\" + userInput + \".txt\");\n" +
            "        File f = new File(\"/var/data/\" + userInput + \".csv\");\n" +
            "        while (true) {\n" +
            "            System.out.println(\"running forever\");\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    public void doWork(String input) {\n" +
            "        if (true) { System.out.println(\"dead branch\"); }\n" +
            "        if (false) { System.out.println(\"dead code\"); }\n" +
            "        if (input == input) { System.out.println(\"always\"); }\n" +
            "        if (input != input) { System.out.println(\"never\"); }\n" +
            "        try {\n" +
            "            int x = 1 / 0;\n" +
            "        } catch (Exception e) {\n" +
            "        }\n" +
            "        double a = 1.0;\n" +
            "        if (a == 1.0) { System.out.println(\"oops\"); }\n" +
            "        Object o = new Integer(42);\n" +
            "        String s = (String) o;\n" +
            "        List<String> list = new ArrayList<>(Arrays.asList(\"a\",\"b\",\"c\"));\n" +
            "        for (String item : list) {\n" +
            "            if (\"b\".equals(item)) list.remove(item);\n" +
            "        }\n" +
            "        String p = maybeNull();\n" +
            "        int len = p.length();\n" +
            "        String x = request.getParameter(\"name\");\n" +
            "        response.getWriter().write(request.getParameter(\"name\"));\n" +
            "        Runtime.getRuntime().exec(\"ls \" + userInput);\n" +
            "        log.info(\"user logged in with password=\" + password);\n" +
            "        MessageDigest md = MessageDigest.getInstance(\"MD5\");\n" +
            "        Cipher c = Cipher.getInstance(\"DES\");\n" +
            "        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));\n" +
            "        Object obj = ois.readObject();\n" +
            "    }\n" +
            "\n" +
            "    private String maybeNull() { return null; }\n" +
            "    private String password = \"p@ssw0rd\";\n" +
            "    private String request = null;\n" +
            "    private String response = null;\n" +
            "    private byte[] data;\n" +
            "}\n";
}
