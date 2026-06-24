package com.codereview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {

    /**
     * 页码（从1开始）
     */
    @Builder.Default
    private int page = 1;

    /**
     * 每页条数
     */
    @Builder.Default
    private int pageSize = 20;

    /**
     * 排序字段
     */
    private String sortBy;

    /**
     * 排序方向：asc/desc
     */
    @Builder.Default
    private String sortOrder = "desc";

    /**
     * 验证并修正分页参数
     */
    public void validate() {
        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1) {
            pageSize = 20;
        }
        if (pageSize > 100) {
            pageSize = 100;
        }
        if (!"asc".equalsIgnoreCase(sortOrder) && !"desc".equalsIgnoreCase(sortOrder)) {
            sortOrder = "desc";
        }
    }

    /**
     * 获取MyBatis-Plus的offset
     */
    public long getOffset() {
        return (page - 1) * pageSize;
    }
}
