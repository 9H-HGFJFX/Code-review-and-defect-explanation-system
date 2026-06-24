package com.codereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建规则请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRuleRequest {

    /**
     * 规则名称
     */
    @NotBlank(message = "name is required")
    private String name;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 分类
     */
    @NotBlank(message = "category is required")
    private String category;

    /**
     * 严重程度
     */
    @NotBlank(message = "severity is required")
    private String severity;

    /**
     * 匹配模式
     */
    private String pattern;

    /**
     * 是否启用
     */
    private Boolean enabled;
}
