package com.codereview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审查问题（缺陷）实体类
 */
@Data
@TableName("issue")
public class Issue {
    
    /** 问题ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 所属审查记录ID */
    private Long reviewId;
    
    /** 触发的规则ID */
    private Long ruleId;
    
    /** 严重级别：CRITICAL/ERROR/WARNING/SUGGESTION */
    private String severity;
    
    /** 问题所在行号 */
    private Integer lineNumber;
    
    /** 问题描述 */
    private String description;
    
    /** 修复建议 */
    private String suggestion;
    
    /** 修改前代码片段 */
    private String codeBefore;
    
    /** 修改后代码示例 */
    private String codeAfter;
    
    /** 逻辑删除 */
    @TableLogic
    private Integer isDeleted;
    
    // ==================== Severity Constants ====================
    public static final String SEVERITY_CRITICAL = "CRITICAL";
    public static final String SEVERITY_ERROR = "ERROR";
    public static final String SEVERITY_WARNING = "WARNING";
    public static final String SEVERITY_SUGGESTION = "SUGGESTION";
}
