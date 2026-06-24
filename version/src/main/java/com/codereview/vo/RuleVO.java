package com.codereview.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 规则VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleVO {

    /**
     * 规则ID
     */
    private Long ruleId;

    /**
     * 规则名称
     */
    private String name;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 分类
     */
    private String category;

    /**
     * 严重程度
     */
    private String severity;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 创建者ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
