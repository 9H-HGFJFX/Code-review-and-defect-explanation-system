package com.codereview.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.codereview.common.enums.RuleCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 代码审查规则实体类
 * 定义一条代码审查规则
 * 
 * @author code-review-team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("review_rule")
public class ReviewRule {

    /**
     * 规则ID - 主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 规则唯一标识
     */
    private String ruleId;

    /**
     * 规则名称
     */
    private String name;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 规则类别
     * @see com.codereview.common.enums.RuleCategory
     */
    private RuleCategory category;

    /**
     * 规则严重程度
     */
    private String severity;

    /**
     * 适用语言（逗号分隔，如：java,python）
     */
    private String languages;

    /**
     * 规则配置（JSON格式）
     */
    private String config;

    /**
     * 正则表达式模式
     */
    private String pattern;

    /**
     * 规则是否启用
     */
    private Boolean enabled;

    /**
     * 规则优先级（数字越小优先级越高）
     */
    private Integer priority;

    /**
     * 规则版本
     */
    private String version;

    /**
     * 是否为内置规则
     */
    private Boolean builtIn;

    /**
     * 创建人ID
     */
    private Long creatorId;

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
