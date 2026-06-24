package com.codereview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 代码缺陷实体类
 * 对应数据库中的 issue 表
 */
@Data
@TableName("issue")
public class Issue {

    /**
     * 缺陷ID，主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联任务ID，外键关联 review_task 表
     */
    private Long taskId;

    /**
     * 严重程度：1=严重，2=高，3=中，4=低
     */
    private Integer severity;

    /**
     * 缺陷分类（如：安全漏洞、代码风格等）
     */
    private String category;

    /**
     * 缺陷所在文件路径
     */
    private String filePath;

    /**
     * 缺陷所在行号
     */
    private Integer lineNumber;

    /**
     * 缺陷详细描述
     */
    private String description;

    /**
     * 修复建议
     */
    private String suggestion;

    /**
     * 缺陷状态：0=未处理，1=已分配，2=已修复，3=已关闭
     */
    private Integer status;

    /**
     * 分配给谁（用户ID），外键关联 user 表
     */
    private Long assigneeId;

    /**
     * 分配人姓名
     */
    private String assigneeName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

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
