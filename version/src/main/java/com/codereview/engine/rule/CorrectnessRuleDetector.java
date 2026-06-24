package com.codereview.engine.rule;

import com.codereview.common.enums.RuleCategory;
import com.codereview.engine.parser.SourceFile;
import com.codereview.entity.CodeIssue;
import com.codereview.entity.ReviewRule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 正确性规则检测器（默认实现）
 * 
 * @author code-review-team
 */
@Component
public class CorrectnessRuleDetector implements RuleDetector {

    @Override
    public RuleCategory getCategory() {
        return RuleCategory.CORRECTNESS;
    }

    @Override
    public List<CodeIssue> detect(SourceFile file, ReviewRule rule) {
        // 简化实现，委托给规则引擎处理
        return List.of();
    }
}
