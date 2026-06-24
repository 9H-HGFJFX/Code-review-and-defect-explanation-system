package com.codereview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 代码审查任务实体类
 * 对应数据库中的 review_task 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("review_task")
public class ReviewTask {

    /**
     * 任务ID，主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务标题
     */
    private String title;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 任务状态（枚举值）
     */
    private Integer status;

    /**
     * 提交人ID，外键关联 user 表
     */
    private Long submitterId;

    /**
     * 审核人ID（教师），外键关联 user 表
     */
    private Long reviewerId;

    /**
     * 所属班级ID，外键关联 class 表
     */
    private Long classId;

    /**
     * 项目ID（可选）
     */
    private Long projectId;

    /**
     * 源码路径
     */
    private String sourcePath;

    /**
     * 扫描结果摘要（JSON格式）
     */
    private String resultSummary;

    /**
     * 截止时间
     */
    private LocalDateTime deadline;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 持续时间（秒）
     */
    private Long durationSeconds;

    /**
     * 规则集（逗号分隔的规则ID）
     */
    private String ruleSet;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 创建时间（别名，impl用）
     */
    public LocalDateTime getCreatedAt() {
        return createTime;
    }

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 更新时间（别名，impl用）
     */
    public LocalDateTime getUpdatedAt() {
        return updateTime;
    }

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer deleted;
}
