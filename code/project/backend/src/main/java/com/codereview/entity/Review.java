package com.codereview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审查记录实体类
 */
@Data
@TableName("review")
public class Review {
    
    /** 记录ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 提交用户ID */
    private Long userId;
    
    /** 原始代码内容 */
    private String codeContent;
    
    /** 文件名 */
    private String fileName;
    
    /** 代码行数 */
    private Integer lineCount;
    
    /** 审查时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime reviewTime;
    
    /** 状态：PENDING/COMPLETED/FAILED */
    private String status;
    
    /** 异步任务ID */
    private String taskId;
    
    /** 逻辑删除 */
    @TableLogic
    private Integer isDeleted;
    
    // ==================== Status Constants ====================
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
}
