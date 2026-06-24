package com.codereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新缺陷状态请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateIssueStatusRequest {

    /**
     * 新状态
     */
    @NotBlank(message = "status is required")
    private String status;
}
