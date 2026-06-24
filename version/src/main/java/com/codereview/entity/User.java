package com.codereview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库中的 user 表
 */
@Data
@TableName("user")
public class User {

    /**
     * 用户ID，主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名，唯一约束
     */
    private String username;

    /**
     * 邮箱，唯一约束
     */
    private String email;

    /**
     * 密码哈希值
     */
    private String passwordHash;

    /**
     * 角色：1=超管，2=教师，3=学生
     */
    private Integer role;

    /**
     * 所属班级ID，外键关联 class 表
     */
    private Long classId;

    /**
     * 逻辑删除标记：0=未删除，1=已删除
     */
    @TableLogic
    private Integer isDeleted;

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