package com.codereview.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 缺陷VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueVO {

    /**
     * 缺陷ID
     */
    private Long issueId;

    /**
     * 关联任务ID
     */
    private Long taskId;

    /**
     * 标题
     */
    private String title;

    /**
     * 描述
     */
    private String description;

    /**
     * 严重程度
     */
    private String severity;

    /**
     * 状态
     */
    private String status;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 行号
     */
    private Integer lineNumber;

    /**
     * 负责人ID
     */
    private Long assigneeId;

    /**
     * 负责人名称
     */
    private String assigneeName;

    /**
     * 创建者ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
