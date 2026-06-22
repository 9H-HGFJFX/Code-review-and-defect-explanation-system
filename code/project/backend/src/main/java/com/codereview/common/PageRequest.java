package com.codereview.common;

import lombok.Data;
import java.io.Serializable;

/**
 * 分页请求参数封装
 */
@Data
public class PageRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 当前页码 */
    private Integer current = 1;
    
    /** 每页条数 */
    private Integer size = 10;
    
    /** 排序字段 */
    private String sortField;
    
    /** 排序方向：asc/desc */
    private String sortOrder;
    
    public void setPage(Integer page) {
        this.current = page;
    }
    
    public void setLimit(Integer limit) {
        this.size = limit;
    }
}
