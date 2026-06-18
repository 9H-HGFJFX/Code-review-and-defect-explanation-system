package com.codeaudit.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 审查结果 VO
 */
@Data
public class ReviewResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long reviewId;
    private String fileName;
    private Integer lineCount;
    private Integer issueCount;
    private String status;
    private Long costMs;
    private LocalDateTime reviewTime;

    /** 问题总数统计 */
    private SeverityStats stats;
    /** 问题列表（已按严重级别从高到低 + 行号排序） */
    private List<IssueVO> issues;
    /** 安全风险摘要 */
    private List<IssueVO> securityIssues;
}
