package com.codereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建班级请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClassRequest {

    /**
     * 班级名称
     */
    @NotBlank(message = "name is required")
    private String name;

    /**
     * 班主任ID
     */
    private Long teacherId;

    /**
     * 班级描述
     */
    private String description;
}
