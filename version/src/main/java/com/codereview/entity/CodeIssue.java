package com.codereview.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.codereview.common.enums.IssueSeverity;
import com.codereview.common.enums.IssueStatus;
import com.codereview.common.enums.RuleCategory;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 代码缺陷/问题实体类
 * 代表代码审查中发现的一个问题
 * 
 * @author code-review-team
 */
@Data
@TableName("code_issue")
public class CodeIssue {

    /**
     * 问题ID - 主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的任务ID
     */
    private Long taskId;

    /**
     * 问题所在文件路径
     */
    private String filePath;

    /**
     * 问题所在行号
     */
    private Integer lineNumber;

    /**
     * 问题所在列号
     */
    private Integer columnNumber;

    /**
     * 问题类型/规则ID
     */
    private String ruleId;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则类别
     * @see com.codereview.common.enums.RuleCategory
     */
    private RuleCategory category;

    /**
     * 问题严重程度
     * @see com.codereview.common.enums.IssueSeverity
     */
    private IssueSeverity severity;

    /**
     * 问题状态
     * @see com.codereview.common.enums.IssueStatus
     */
    private IssueStatus status;

    /**
     * 问题描述
     */
    private String description;

    /**
     * 问题代码片段
     */
    private String codeSnippet;

    /**
     * 修复建议
     */
    private String suggestion;

    /**
     * 问题责任人ID
     */
    private Long assigneeId;

    /**
     * 问题责任人姓名
     */
    private String assigneeName;

    /**
     * 备注
     */
    private String remark;

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
