package com.codereview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 添加学生到班级请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddStudentRequest {

    /**
     * 学生ID
     */
    @NotNull(message = "studentId is required")
    private Long studentId;

    /**
     * 班级ID（可选，如果路径参数有则不需要）
     */
    private Long classId;
}
