package com.codereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 规则创建/更新请求DTO
 */
@Data
public class RuleRequest {
    
    @NotBlank(message = "规则分类不能为空")
    private String category;
    
    @NotBlank(message = "规则名称不能为空")
    private String name;
    
    @NotBlank(message = "匹配模式类型不能为空")
    private String patternType;
    
    /** 具体匹配表达式 */
    private String pattern;
    
    /** 默认严重级别 */
    @NotBlank(message = "严重级别不能为空")
    private String severity;
    
    /** 班级ID，为空代表全局公共规则 */
    private Long classId;
    
    /** 规则描述 */
    private String description;
    
    /** 修复建议模板 */
    private String suggestionTemplate;
    
    /** 是否启用 */
    private Integer enabled = 1;
}
