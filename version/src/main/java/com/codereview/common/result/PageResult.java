package com.codereview.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应结果封装
 *
 * @param <T> 列表项数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据列表
     */
    private List<T> list;

    /**
     * 分页信息
     */
    private Pagination pagination;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pagination implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 当前页码（从1开始）
         */
        private int page;

        /**
         * 每页条数
         */
        private int pageSize;

        /**
         * 总记录数
         */
        private long total;

        /**
         * 总页数
         */
        private int totalPages;

        /**
         * 创建分页信息
         */
        public static Pagination of(int page, int pageSize, long total) {
            int totalPages = (int) Math.ceil((double) total / pageSize);
            return new Pagination(page, pageSize, total, totalPages);
        }
    }
}
