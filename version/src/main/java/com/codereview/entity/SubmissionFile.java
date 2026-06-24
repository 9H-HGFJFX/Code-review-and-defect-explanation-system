package com.codereview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提交文件实体类
 * 代表一次审查任务提交的文件
 * 
 * @author code-review-team
 */
@Data
@TableName("submission_file")
public class SubmissionFile {

    /**
     * 文件ID - 主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的任务ID
     */
    private Long taskId;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件扩展名
     */
    private String extension;

    /**
     * 编程语言
     */
    private String language;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件行数
     */
    private Integer lineCount;

    /**
     * 文件内容（可能存储在文件系统）
     */
    private String contentPath;

    /**
     * 文件状态（SUCCESS, FAILED, PARSED）
     */
    private String status;

    /**
     * 解析失败原因
     */
    private String failReason;

    /**
     * 检测到的问题数量
     */
    private Integer issueCount;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer deleted;
}
