package com.codereview.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务详情VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDetailVO {

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 任务标题
     */
    private String title;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 状态
     */
    private String status;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 创建者ID
     */
    private Long createdBy;

    /**
     * 创建者名称
     */
    private String creatorName;

    /**
     * 仓库URL
     */
    private String repoUrl;

    /**
     * 截止时间
     */
    private LocalDateTime deadline;

    /**
     * 规则ID列表
     */
    private List<Long> ruleIds;

    /**
     * 缺陷数量
     */
    private Integer issueCount;

    /**
     * 已解决缺陷数量
     */
    private Integer resolvedCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
