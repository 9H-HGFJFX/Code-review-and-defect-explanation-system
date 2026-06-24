package com.codereview.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务VO（列表展示）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskVO {

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 任务标题
     */
    private String title;

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
     * 缺陷数量
     */
    private Integer issueCount;

    /**
     * 截止时间
     */
    private LocalDateTime deadline;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
