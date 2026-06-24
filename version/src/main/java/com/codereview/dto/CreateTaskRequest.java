package com.codereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建审查任务请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequest {

    /**
     * 任务标题
     */
    @NotBlank(message = "title is required")
    private String title;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 截止时间
     */
    private String deadline;

    /**
     * 规则ID列表
     */
    private Long[] ruleIds;

    /**
     * 仓库URL
     */
    private String repoUrl;
}
