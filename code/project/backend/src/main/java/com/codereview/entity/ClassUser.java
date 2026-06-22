package com.codereview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 班级成员中间表实体类
 */
@Data
@TableName("class_user")
public class ClassUser {
    
    /** ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 班级ID */
    private Long classId;
    
    /** 用户ID */
    private Long userId;
    
    /** 加入时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime joinTime;
    
    /** 逻辑删除 */
    @TableLogic
    private Integer isDeleted;
}
