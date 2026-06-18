package com.codeaudit.engine.detector;

import com.codeaudit.engine.IssueDraft;
import com.codeaudit.engine.ReviewContext;
import com.codeaudit.engine.executor.RuleExecutor;
import com.codeaudit.entity.Rule;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 资源泄漏检测器
 * 启发式：
 *   - new FileInputStream/FileOutputStream/FileReader/FileWriter/Scanner/Socket/Connection/BufferedXxx
 *     赋值给变量后，若没在 try-with-resources 内、且方法体内也没看到 close()/try-with-resources，则报告
 *   - 仅做 best-effort 启发式
 */
@Component("resourceLeakDetector")
public class ResourceLeakDetector implements RuleExecutor {

    private static final Set<String> RESOURCE_TYPES = Set.of(
            "FileInputStream", "FileOutputStream", "FileReader", "FileWriter",
            "BufferedReader", "BufferedWriter", "InputStreamReader", "OutputStreamWriter",
            "Scanner", "Socket", "ServerSocket", "Connection", "Statement",
            "PreparedStatement", "ResultSet", "ZipFile", "ZipInputStream", "ZipOutputStream"
    );

    @Override
    public void execute(ReviewContext ctx, Rule rule) {
        if (!"DEFECT_RESOURCE_LEAK".equals(rule.getCode())) return;
        CompilationUnit cu = ctx.getCompilationUnit();
        if (cu == null) return;

        cu.findAll(VariableDeclarationExpr.class).forEach(vd -> {
            vd.getVariables().forEach(v -> {
                if (!v.getInitializer().isPresent()) return;
                if (!(v.getInitializer().get() instanceof ObjectCreationExpr oc)) return;
                String type = oc.getType().getNameAsString();
                if (!RESOURCE_TYPES.contains(type)) return;
                String varName = v.getNameAsString();
                int line = vd.getBegin().map(p -> p.line).orElse(0);

                // 包裹在 try-with-resources 内的，参数名是 ResourceSpecification
                if (isInTryWithResources(cu, varName)) return;

                // 代码内出现 varName.close() 也视为关闭
                if (ctx.getPreprocessedCode().contains(varName + ".close()")) return;

                ctx.getIssues().add(IssueDraft.builder()
                        .ruleId(rule.getId()).ruleName(rule.getName())
                        .category("DEFECT").severity(rule.getSeverity())
                        .lineNumber(line)
                        .description("资源 '" + type + "' 在使用后未关闭，可能造成资源泄漏")
                        .suggestion("使用 try-with-resources 自动关闭：try (" + type + " " + varName + " = ...) { ... }")
                        .codeBefore(type + " " + varName + " = new " + type + "(...);")
                        .codeAfter("try (" + type + " " + varName + " = new " + type + "(...)) {\n    // use " + varName + "\n}")
                        .build());
            });
        });
    }

    private boolean isInTryWithResources(CompilationUnit cu, String varName) {
        for (TryStmt t : cu.findAll(TryStmt.class)) {
            t.getResources().forEach(r -> {
                if (r.toString().contains(varName)) { /* 视为在 twr 内 */ }
            });
            for (com.github.javaparser.ast.expr.Expression r : t.getResources()) {
                String s = r.toString();
                if (s.startsWith(varName + " ") || s.contains(" " + varName + " ") || s.contains("=" + varName)
                        || s.endsWith(" " + varName) || s.contains(varName)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override public String category() { return "DEFECT"; }
    @Override public String patternType() { return "AST"; }
}
