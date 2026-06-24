package com.codereview.engine.rule;

import com.codereview.engine.parser.SourceFile;
import com.codereview.entity.CodeIssue;
import com.codereview.entity.ReviewRule;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 默认规则检测器
 * 用于未匹配到特定检测器的情况
 * 
 * @author code-review-team
 */
@Component
public class DefaultRuleDetector implements RuleDetector {

    @Override
    public List<CodeIssue> detect(SourceFile file, ReviewRule rule) {
        // 默认实现：使用规则引擎的正则表达式匹配
        if (rule.getPattern() != null && !rule.getPattern().isEmpty()) {
            // 委托给规则引擎处理
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }
}
