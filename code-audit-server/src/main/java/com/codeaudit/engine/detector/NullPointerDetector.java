package com.codeaudit.engine.detector;

import com.codeaudit.engine.IssueDraft;
import com.codeaudit.engine.ReviewContext;
import com.codeaudit.engine.executor.RuleExecutor;
import com.codeaudit.entity.Rule;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 空指针风险检测器
 * 启发式策略：
 *   1) 局部变量经 if (x != null) 判空后才视为安全
 *   2) 字面量赋值（"abc"、Collections.emptyList()、new ArrayList<>()）视为非空
 *   3) 方法调用赋值（x = someMethod()）视为可疑，需要判空
 *   4) 形参/字段无法证明非空，调用方法时也认为可能 NPE
 *
 * 出于性能与可用性平衡，仅做"最有可能"的检测，不做完整数据流分析。
 */
@Component("nullPointerDetector")
public class NullPointerDetector implements RuleExecutor {

    @Override
    public void execute(ReviewContext ctx, Rule rule) {
        if (!"DEFECT_NULL_POINTER".equals(rule.getCode())) return;
        CompilationUnit cu = ctx.getCompilationUnit();
        if (cu == null) return;

        // 简单模型：仅对"方法调用表达式"且其作用域对象是 NameExpr / FieldAccessExpr 时
        // 启发式判断是否经过 null 检查
        cu.accept(new com.github.javaparser.ast.visitor.VoidVisitorAdapter<>() {
            @Override
            public void visit(MethodCallExpr mc, Object arg) {
                Optional<Expression> scope = mc.getScope();
                if (scope.isEmpty()) return;
                Expression s = scope.get();
                String varName = extractVarName(s);
                if (varName == null) return;
                int line = mc.getBegin().map(p -> p.line).orElse(0);
                if (isProbablyNull(cu, varName, line)) {
                    ctx.getIssues().add(IssueDraft.builder()
                            .ruleId(rule.getId()).ruleName(rule.getName())
                            .category("DEFECT").severity(rule.getSeverity())
                            .lineNumber(line)
                            .description("变量 '" + varName + "' 可能为 null，建议在使用前进行 null 判断或使用 Optional")
                            .suggestion("使用前判空：if (" + varName + " != null) { ... }；或采用 Optional.ofNullable(" + varName + ").ifPresent(...)")
                            .codeBefore(varName + "." + mc.getNameAsString() + "(...)")
                            .codeAfter("if (" + varName + " != null) {\n    " + varName + "." + mc.getNameAsString() + "(...)\n}")
                            .build());
                }
            }
        }, null);
    }

    private String extractVarName(Expression e) {
        if (e instanceof NameExpr n) return n.getNameAsString();
        if (e instanceof FieldAccessExpr f) return f.toString(); // this.field 也带上
        return null;
    }

    private boolean isProbablyNull(CompilationUnit cu, String varName, int callLine) {
        // 在调用行之前的同一方法体内：
        // 1) 出现过 if (varName != null) / Objects.nonNull(varName)  -> 安全
        // 2) 字面量赋值 -> 非空，安全
        // 3) 仅 new Xxx(...) 赋值 -> 非空，安全
        // 4) 方法调用赋值 -> 视为可疑（我们无法证明非空）
        // 5) 形参/字段：视为可疑

        // 简化：扫描整个 cu，找 varName 出现的位置
        // 实际生产应做方法内数据流分析；这里采用启发式
        for (com.github.javaparser.ast.stmt.IfStmt ifStmt : cu.findAll(com.github.javaparser.ast.stmt.IfStmt.class)) {
            String cond = ifStmt.getCondition().toString();
            if (cond.contains(varName + " != null") || cond.contains("Objects.nonNull(" + varName + ")")) {
                // 在判空 if 块的子句之前/之后调用都视为安全
                int ifStart = ifStmt.getBegin().map(p -> p.line).orElse(0);
                int ifEnd = ifStmt.getEnd().map(p -> p.line).orElse(Integer.MAX_VALUE);
                if (callLine >= ifStart && callLine <= ifEnd) return false;
            }
        }

        // 是否有字面量/新对象赋值
        for (VariableDeclarationExpr vd : cu.findAll(VariableDeclarationExpr.class)) {
            for (VariableDeclarator v : vd.getVariables()) {
                if (!varName.equals(v.getNameAsString())) continue;
                int line = vd.getBegin().map(p -> p.line).orElse(0);
                if (line > callLine) break;
                Optional<Expression> init = v.getInitializer();
                if (init.isEmpty()) continue;
                Expression e = init.get();
                if (e instanceof StringLiteralExpr
                        || e instanceof IntegerLiteralExpr
                        || e instanceof BooleanLiteralExpr
                        || e instanceof CharLiteralExpr
                        || e instanceof ObjectCreationExpr
                        || e instanceof ArrayCreationExpr
                        || e instanceof ArrayInitializerExpr
                        || e instanceof EnclosedExpr) {
                    return false; // 非空
                }
            }
        }
        return true;
    }

    @Override public String category() { return "DEFECT"; }
    @Override public String patternType() { return "AST"; }
}
