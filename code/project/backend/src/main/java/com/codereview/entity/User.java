package com.codereview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@TableName("user")
public class User {
    
    /** 用户ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 用户名 */
    private String username;
    
    /** 密码（BCrypt加密） */
    private String password;
    
    /** 角色：STUDENT/TEACHER/ADMIN */
    private String role;
    
    /** 邮箱 */
    private String email;
    
    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /** 逻辑删除：0正常，1已删除 */
    @TableLogic
    private Integer isDeleted;
    
    // ==================== Role Constants ====================
    public static final String ROLE_STUDENT = "STUDENT";
    public static final String ROLE_TEACHER = "TEACHER";
    public static final String ROLE_ADMIN = "ADMIN";
}
