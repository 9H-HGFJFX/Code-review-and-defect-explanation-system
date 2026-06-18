package com.codeaudit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@TableName("issue")
public class Issue implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("review_id")
    private Long reviewId;

    @TableField("rule_id")
    private Long ruleId;

    @TableField("rule_name")
    private String ruleName;

    /** STYLE / DEFECT / SECURITY */
    private String category;

    /** CRITICAL / ERROR / WARNING / SUGGESTION */
    private String severity;

    @TableField("line_number")
    private Integer lineNumber;

    @TableField("end_line")
    private Integer endLine;

    @TableField("col_number")
    private Integer colNumber;

    private String description;

    private String suggestion;

    @TableField("code_before")
    private String codeBefore;

    @TableField("code_after")
    private String codeAfter;

    @TableField("ai_explain")
    private String aiExplain;

    @JsonIgnore
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
