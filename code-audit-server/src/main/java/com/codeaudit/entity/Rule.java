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
import java.time.LocalDateTime;

@Data
@TableName("rule")
public class Rule implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** STYLE / DEFECT / SECURITY */
    private String category;

    private String name;

    private String code;

    /** REGEX / AST */
    @TableField("pattern_type")
    private String patternType;

    private String severity;

    private String description;

    @TableField("suggestion_template")
    private String suggestionTemplate;

    @TableField("executor_bean")
    private String executorBean;

    private Integer enabled;

    @TableField("is_builtin")
    private Integer isBuiltin;

    @TableField("class_id")
    private Long classId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @JsonIgnore
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
