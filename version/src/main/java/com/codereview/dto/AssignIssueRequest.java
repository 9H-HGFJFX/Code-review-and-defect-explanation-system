package com.codereview.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分配缺陷请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignIssueRequest {

    /**
     * 被分配的用户ID
     */
    @NotNull(message = "assigneeId is required")
    private Long assigneeId;
}
