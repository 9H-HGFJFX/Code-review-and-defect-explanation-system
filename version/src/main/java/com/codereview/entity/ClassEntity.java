package com.codereview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 班级实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("class")
public class ClassEntity {

    /**
     * 班级ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 班级名称
     */
    private String name;

    /**
     * 班主任ID
     */
    private Long teacherId;

    /**
     * 班级描述
     */
    private String description;

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
