package com.codereview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级信息实体类
 * 对应数据库中的 class 表
 */
@Data
@TableName("class")
public class ClassInfo {

    /**
     * 班级ID，主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 班级名称
     */
    private String name;

    /**
     * 班主任ID，外键关联 user 表
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
}