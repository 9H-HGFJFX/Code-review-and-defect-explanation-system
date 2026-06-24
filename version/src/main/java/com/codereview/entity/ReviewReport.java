package com.codereview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审查报告实体类
 * 代表一次审查生成的分析报告
 * 
 * @author code-review-team
 */
@Data
@TableName("review_report")
public class ReviewReport {

    /**
     * 报告ID - 主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的任务ID
     */
    private Long taskId;

    /**
     * 报告标题
     */
    private String title;

    /**
     * 报告类型（HTML, PDF, JSON）
     */
    private String reportType;

    /**
     * 报告文件路径
     */
    private String filePath;

    /**
     * 报告文件大小（字节）
     */
    private Long fileSize;

    /**
     * 报告摘要（JSON格式）
     */
    private String summary;

    /**
     * 报告生成时间
     */
    private LocalDateTime generateTime;

    /**
     * 创建人ID
     */
    private Long creatorId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer deleted;
}
