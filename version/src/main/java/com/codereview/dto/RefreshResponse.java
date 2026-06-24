package com.codereview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 刷新Token响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshResponse {

    /**
     * 新的Access Token
     */
    private String accessToken;

    /**
     * 过期时间（秒）
     */
    private long expiresIn;
}
