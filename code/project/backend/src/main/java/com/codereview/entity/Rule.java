package com.codereview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审查规则实体类
 */
@Data
@TableName("rule")
public class Rule {
    
    /** 规则ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 分类：STYLE/DEFECT/SECURITY */
    private String category;
    
    /** 规则名称 */
    private String name;
    
    /** 匹配模式类型：REGEX/AST */
    private String patternType;
    
    /** 具体匹配表达式 */
    private String pattern;
    
    /** 默认严重级别 */
    private String severity;
    
    /** 班级ID，为空代表全局公共规则 */
    private Long classId;
    
    /** 规则描述 */
    private String description;
    
    /** 修复建议模板 */
    private String suggestionTemplate;
    
    /** 是否启用：1启用，0禁用 */
    private Integer enabled;
    
    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /** 逻辑删除 */
    @TableLogic
    private Integer isDeleted;
    
    // ==================== Category Constants ====================
    public static final String CATEGORY_STYLE = "STYLE";
    public static final String CATEGORY_DEFECT = "DEFECT";
    public static final String CATEGORY_SECURITY = "SECURITY";
    
    // ==================== Pattern Type Constants ====================
    public static final String PATTERN_REGEX = "REGEX";
    public static final String PATTERN_AST = "AST";
}
