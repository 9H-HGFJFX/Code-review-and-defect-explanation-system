package com.codereview.common;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 分页响应结果封装
 */
@Data
public class PageResponse<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 状态码 */
    private Integer code;
    
    /** 消息 */
    private String message;
    
    /** 数据列表 */
    private List<T> records;
    
    /** 总条数 */
    private Long total;
    
    /** 当前页 */
    private Long current;
    
    /** 每页条数 */
    private Long size;
    
    public PageResponse() {
    }
    
    public PageResponse(List<T> records, Long total, Long current, Long size) {
        this.code = 200;
        this.message = "success";
        this.records = records;
        this.total = total;
        this.current = current;
        this.size = size;
    }
    
    public static <T> PageResponse<T> ok(List<T> records, Long total, Long current, Long size) {
        return new PageResponse<>(records, total, current, size);
    }
}
