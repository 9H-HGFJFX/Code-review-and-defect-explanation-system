package com.codeaudit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RuleSaveReq {

    @NotBlank(message = "规则名称不能为空")
    private String name;

    @NotBlank(message = "规则编码不能为空")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "编码需全大写下划线")
    private String code;

    @NotBlank(message = "分类不能为空")
    @Pattern(regexp = "^(STYLE|DEFECT|SECURITY)$", message = "分类必须为 STYLE/DEFECT/SECURITY")
    private String category;

    @NotBlank(message = "严重级别不能为空")
    @Pattern(regexp = "^(CRITICAL|ERROR|WARNING|SUGGESTION)$", message = "严重级别非法")
    private String severity;

    /** REGEX / AST */
    @NotBlank(message = "匹配类型不能为空")
    private String patternType;

    private String pattern;
    private String description;
    private String suggestionTemplate;
    private String executorBean;
    private Long classId;
    private Integer enabled = 1;
}
