package com.codereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 刷新Token请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshRequest {

    /**
     * Refresh Token
     */
    @NotBlank(message = "refreshToken is required")
    private String refreshToken;
}
