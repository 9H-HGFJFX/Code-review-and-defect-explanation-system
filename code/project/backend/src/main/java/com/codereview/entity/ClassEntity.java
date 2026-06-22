package com.codereview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 班级实体类
 */
@Data
@TableName("class")
public class ClassEntity {
    
    /** 班级ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 班级名称 */
    private String className;
    
    /** 班级描述 */
    private String description;
    
    /** 管理教师ID */
    private Long teacherId;
    
    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /** 逻辑删除 */
    @TableLogic
    private Integer isDeleted;
}
