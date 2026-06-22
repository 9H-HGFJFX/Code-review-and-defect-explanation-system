package com.codeaudit.engine;

import com.codeaudit.entity.Issue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 引擎在内存中暂存的问题（落库前形态）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueDraft implements Serializable, Comparable<IssueDraft> {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long ruleId;
    private String ruleName;
    private String category;       // STYLE / DEFECT / SECURITY
    private String severity;       // CRITICAL / ERROR / WARNING / SUGGESTION
    private Integer lineNumber;
    private Integer endLine;
    private Integer colNumber;
    private String description;
    private String suggestion;
    private String codeBefore;
    private String codeAfter;

    public Issue toEntity(Long reviewId) {
        Issue i = new Issue();
        i.setReviewId(reviewId);
        i.setRuleId(ruleId);
        i.setRuleName(ruleName);
        i.setCategory(category);
        i.setSeverity(severity);
        i.setLineNumber(lineNumber == null ? 0 : lineNumber);
        i.setEndLine(endLine);
        i.setColNumber(colNumber);
        i.setDescription(description);
        i.setSuggestion(suggestion);
        i.setCodeBefore(codeBefore);
        i.setCodeAfter(codeAfter);
        return i;
    }

    /** 严重级别权重：CRITICAL=0, ERROR=1, WARNING=2, SUGGESTION=3（数字越小越靠前） */
    private static int weight(String severity) {
        return switch (severity == null ? "" : severity) {
            case "CRITICAL" -> 0;
            case "ERROR" -> 1;
            case "WARNING" -> 2;
            case "SUGGESTION" -> 3;
            default -> 4;
        };
    }

    @Override
    public int compareTo(IssueDraft o) {
        int w1 = weight(this.severity);
        int w2 = weight(o.severity);
        if (w1 != w2) return Integer.compare(w1, w2);
        int l1 = this.lineNumber == null ? 0 : this.lineNumber;
        int l2 = o.lineNumber == null ? 0 : o.lineNumber;
        return Integer.compare(l1, l2);
    }
}
