package com.codereview.engine.rule;

import com.codereview.engine.parser.SourceFile;
import com.codereview.entity.CodeIssue;
import com.codereview.entity.ReviewRule;

import java.util.List;

/**
 * 规则检测器接口
 * 定义各类别规则检测器的统一接口
 * 
 * @author code-review-team
 */
public interface RuleDetector {

    /**
     * 检测代码问题
     * 
     * @param file 源代码文件
     * @param rule 检测规则
     * @return 检测到的问题列表
     */
    List<CodeIssue> detect(SourceFile file, ReviewRule rule);

    /**
     * 批量检测
     * 
     * @param file 源代码文件
     * @param rules 规则列表
     * @return 检测到的问题列表
     */
    default List<CodeIssue> detectBatch(SourceFile file, List<ReviewRule> rules) {
        return rules.stream()
            .map(rule -> detect(file, rule))
            .flatMap(List::stream)
            .toList();
    }

    /**
     * 获取检测器支持的规则类别
     * 
     * @return 规则类别
     */
    default com.codereview.common.enums.RuleCategory getCategory() {
        return null;
    }
}
