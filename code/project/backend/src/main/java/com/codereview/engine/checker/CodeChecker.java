package com.codereview.engine.checker;

import com.codereview.entity.Issue;
import com.codereview.entity.Rule;
import com.github.javaparser.ast.CompilationUnit;

import java.util.List;

/**
 * 代码检查器接口
 * 所有检查器统一实现此接口
 */
public interface CodeChecker {
    
    /**
     * 执行检查
     * 
     * @param ast 抽象语法树
     * @param sourceCode 源代码
     * @param rules 应用的规则列表
     * @return 发现的问题列表
     */
    List<Issue> check(CompilationUnit ast, String sourceCode, List<Rule> rules);
}
