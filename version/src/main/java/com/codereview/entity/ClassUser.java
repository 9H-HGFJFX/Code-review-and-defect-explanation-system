package com.codereview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 班级用户关联实体类
 * 对应数据库中的 class_user 表（复合主键）
 * 复合主键：class_id + user_id
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("class_user")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ClassUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 班级ID，复合主键组成部分，外键关联 class 表
     */
    @EqualsAndHashCode.Include
    private Long classId;

    /**
     * 用户ID，复合主键组成部分，外键关联 user 表
     */
    @EqualsAndHashCode.Include
    private Long userId;

    /**
     * 班级内角色：2=教师，3=学生
     */
    private Integer roleInClass;

    /**
     * 加入班级时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime joinedAt;
}