package com.codeaudit.engine;

import com.github.javaparser.ast.CompilationUnit;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 引擎执行上下文
 */
@Data
@Builder
public class ReviewContext {
    /** 原始源代码 */
    private String sourceCode;
    /** 预处理后（去 BOM、统一换行）的代码 */
    private String preprocessedCode;
    /** JavaParser 解析结果，失败时为 null */
    private CompilationUnit compilationUnit;
    /** 解析异常信息 */
    private String parseError;
    /** 收集的所有问题 */
    @Builder.Default
    private List<IssueDraft> issues = new ArrayList<>();
    /** 文件名（仅用于上下文展示） */
    private String fileName;
}
