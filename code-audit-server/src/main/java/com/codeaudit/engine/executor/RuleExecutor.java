package com.codeaudit.engine.executor;

import com.codeaudit.engine.ReviewContext;
import com.codeaudit.entity.Rule;

import java.util.List;

/**
 * 规则执行器统一接口
 * 命名/格式/结构/缺陷/安全等所有检查器都实现此接口
 */
public interface RuleExecutor {

    /**
     * 执行检查
     * @param context 审查上下文
     * @param rule    当前触发的规则
     */
    void execute(ReviewContext context, Rule rule);

    /**
     * 当前执行器支持的 category（STYLE / DEFECT / SECURITY）
     */
    String category();

    /**
     * 当前执行器支持的 patternType（AST / REGEX）
     */
    String patternType();

    /**
     * 排除列表 - 有些规则即便启用但当前代码结构不适用，可短路返回
     * 默认实现为空
     */
    default List<String> exclude() { return List.of(); }
}
