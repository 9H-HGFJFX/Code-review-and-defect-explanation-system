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
@TableName("review")
public class Review implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("code_content")
    private String codeContent;

    @TableField("file_name")
    private String fileName;

    @TableField("line_count")
    private Integer lineCount;

    @TableField("issue_count")
    private Integer issueCount;

    /** PENDING / COMPLETED / FAILED */
    private String status;

    @TableField("error_msg")
    private String errorMsg;

    @TableField("review_time")
    private LocalDateTime reviewTime;

    @TableField("cost_ms")
    private Long costMs;

    @JsonIgnore
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
