package com.codeaudit.engine.detector;

import com.codeaudit.engine.IssueDraft;
import com.codeaudit.engine.ReviewContext;
import com.codeaudit.engine.executor.RuleExecutor;
import com.codeaudit.entity.Rule;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * ConcurrentModificationException 风险检测器
 * 规则：DEFECT_CONCURRENT_MODIFICATION
 * 检测在 for-each 循环体内部对同一集合调用 add/remove/put/clear 等修改方法。
 *
 * 说明：这是粗略的"调用同名方法"匹配，不做完整数据流分析，足以覆盖教学场景。
 */
@Component("concurrentModDetector")
public class ConcurrentModDetector implements RuleExecutor {

    /** 视为"修改集合"的方法名 */
    private static final Set<String> MUT_METHODS = Set.of(
            "add", "remove", "addAll", "removeAll", "removeIf", "retainAll",
            "clear", "put", "putIfAbsent", "replace", "replaceAll",
            "compute", "computeIfAbsent", "computeIfPresent", "merge"
    );

    @Override
    public void execute(ReviewContext ctx, Rule rule) {
        if (!"DEFECT_CONCURRENT_MODIFICATION".equals(rule.getCode())) return;
        CompilationUnit cu = ctx.getCompilationUnit();
        if (cu == null) return;

        // 1) for-each 循环
        cu.findAll(ForEachStmt.class).forEach(fe -> {
            String iterName = fe.getIterable().toString();
            int line = fe.getBegin().map(p -> p.line).orElse(0);
            if (bodyMutatesTarget(fe.getBody().toString(), iterName)) {
                ctx.getIssues().add(IssueDraft.builder()
                        .ruleId(rule.getId()).ruleName(rule.getName())
                        .category("DEFECT").severity(rule.getSeverity())
                        .lineNumber(line)
                        .description("在 for-each 循环 '" + iterName + "' 中修改集合，可能抛出 ConcurrentModificationException")
                        .suggestion("使用 Iterator 的 remove() 方法：Iterator<...> it = " + iterName + ".iterator(); while (it.hasNext()) { ... it.remove(); }")
                        .codeBefore("for (T item : " + iterName + ") {\n    " + iterName + ".remove(item); // 抛 CME\n}")
                        .codeAfter("Iterator<T> it = " + iterName + ".iterator();\nwhile (it.hasNext()) {\n    T item = it.next();\n    if (needRemove) it.remove();\n}")
                        .build());
            }
        });

        // 2) 普通 for 循环（按索引遍历 List 时调用 list.add/remove）
        cu.findAll(ForStmt.class).forEach(fs -> {
            int line = fs.getBegin().map(p -> p.line).orElse(0);
            // 提取 for 体内出现的潜在 list 变量名（粗略）
            Set<String> indexTargets = extractListLikeNames(fs.getBody().toString());
            if (bodyCallsMutOnAny(fs.getBody().toString(), indexTargets)) {
                ctx.getIssues().add(IssueDraft.builder()
                        .ruleId(rule.getId()).ruleName(rule.getName())
                        .category("DEFECT").severity(rule.getSeverity())
                        .lineNumber(line)
                        .description("在按索引遍历的 for 循环中调用集合的 add/remove/put 等修改方法，可能导致 ConcurrentModificationException 或索引越界")
                        .suggestion("改为 Iterator 方式遍历并通过 it.remove() 删除；或在循环结束后再统一修改集合")
                        .codeBefore("for (int i = 0; i < list.size(); i++) {\n    list.remove(i); // 越界或跳过元素\n}")
                        .codeAfter("// 推荐 1：倒序遍历删除\nfor (int i = list.size() - 1; i >= 0; i--) {\n    list.remove(i);\n}\n// 推荐 2：使用 Iterator\nfor (Iterator<T> it = list.iterator(); it.hasNext(); ) {\n    T x = it.next();\n    if (cond) it.remove();\n}")
                        .build());
            }
        });
    }

    private boolean bodyMutatesTarget(String body, String iterExpr) {
        // 提取 iterExpr 中最右侧的标识符作为集合变量名
        String name = lastIdentifier(iterExpr);
        if (name == null) return false;
        return bodyCallsMutOnAny(body, Set.of(name));
    }

    private boolean bodyCallsMutOnAny(String body, Set<String> targets) {
        if (targets == null || targets.isEmpty()) return false;
        for (String t : targets) {
            // 匹配 t.add / t.remove / t.put / t.clear 等
            for (String m : MUT_METHODS) {
                if (body.contains(t + "." + m + "(") || body.contains(t + "." + m + " (")
                        || body.contains(" " + t + "." + m) || body.contains("\n" + t + "." + m)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Set<String> extractListLikeNames(String body) {
        // 粗略：找形如 listName.add/remove 的写法，取前面的标识符
        Set<String> out = new HashSet<>();
        for (String m : MUT_METHODS) {
            int idx = 0;
            while ((idx = body.indexOf("." + m + "(", idx)) != -1) {
                int start = idx - 1;
                while (start >= 0 && (Character.isLetterOrDigit(body.charAt(start)) || body.charAt(start) == '_')) start--;
                String name = body.substring(start + 1, idx);
                if (!name.isEmpty() && !name.contains(" ")) out.add(name);
                idx += m.length() + 2;
            }
        }
        return out;
    }

    private String lastIdentifier(String expr) {
        if (expr == null) return null;
        int dot = expr.lastIndexOf('.');
        String tail = dot >= 0 ? expr.substring(dot + 1) : expr;
        // 去掉括号 / 泛型
        int paren = tail.indexOf('(');
        if (paren >= 0) tail = tail.substring(0, paren);
        int gen = tail.indexOf('<');
        if (gen >= 0) tail = tail.substring(0, gen);
        tail = tail.trim();
        return tail.isEmpty() ? null : tail;
    }

    @Override public String category() { return "DEFECT"; }
    @Override public String patternType() { return "AST"; }
}
