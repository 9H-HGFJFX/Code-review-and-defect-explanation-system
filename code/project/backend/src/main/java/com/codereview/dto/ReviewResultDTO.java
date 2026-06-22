package com.codereview.dto;

import com.codereview.entity.Issue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审查结果响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResultDTO {
    
    /** 审查记录ID */
    private Long id;
    
    /** 提交用户ID */
    private Long userId;
    
    /** 用户名 */
    private String username;
    
    /** 文件名 */
    private String fileName;
    
    /** 代码行数 */
    private Integer lineCount;
    
    /** 审查时间 */
    private LocalDateTime reviewTime;
    
    /** 审查状态 */
    private String status;
    
    /** 任务ID（异步任务） */
    private String taskId;
    
    /** 问题列表 */
    private List<Issue> issues;
    
    /** 问题总数 */
    private Integer totalIssues;
    
    /** 严重问题数 */
    private Integer criticalCount;
    
    /** 错误数 */
    private Integer errorCount;
    
    /** 警告数 */
    private Integer warningCount;
    
    /** 建议数 */
    private Integer suggestionCount;
}
