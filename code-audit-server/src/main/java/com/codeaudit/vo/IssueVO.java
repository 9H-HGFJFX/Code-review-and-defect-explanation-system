package com.codeaudit.vo;

import com.codeaudit.entity.Issue;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
public class IssueVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long ruleId;
    private String ruleName;
    private String category;
    private String severity;
    private Integer lineNumber;
    private Integer endLine;
    private Integer colNumber;
    private String description;
    private String suggestion;
    private String codeBefore;
    private String codeAfter;
    private String aiExplain;

    public static IssueVO from(Issue i) {
        IssueVO v = new IssueVO();
        v.id = i.getId();
        v.ruleId = i.getRuleId();
        v.ruleName = i.getRuleName();
        v.category = i.getCategory();
        v.severity = i.getSeverity();
        v.lineNumber = i.getLineNumber();
        v.endLine = i.getEndLine();
        v.colNumber = i.getColNumber();
        v.description = i.getDescription();
        v.suggestion = i.getSuggestion();
        v.codeBefore = i.getCodeBefore();
        v.codeAfter = i.getCodeAfter();
        v.aiExplain = i.getAiExplain();
        return v;
    }
}
