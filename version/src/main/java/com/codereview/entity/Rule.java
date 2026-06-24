package com.codereview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 代码审查规则实体类
 * 对应数据库中的 rule 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("rule")
public class Rule {

    /**
     * 规则ID，主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 规则名称，唯一约束
     */
    private String name;

    /**
     * 分类：1=风格，2=安全，3=性能，4=最佳实践，5=正确性
     */
    private Integer category;

    /**
     * 对应的缺陷严重程度
     */
    private Integer severity;

    /**
     * 匹配模式（JSON格式）
     */
    private String pattern;

    /**
     * 缺陷检测提示信息
     */
    private String message;

    /**
     * 是否启用：1=启用，0=禁用
     */
    private Integer enabled;

    /**
     * 规则版本号
     */
    private Integer version;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}