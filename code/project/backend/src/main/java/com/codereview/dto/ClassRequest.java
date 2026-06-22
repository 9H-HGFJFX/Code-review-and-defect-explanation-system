package com.codereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 班级创建请求DTO
 */
@Data
public class ClassRequest {
    
    @NotBlank(message = "班级名称不能为空")
    private String className;
    
    /** 班级描述 */
    private String description;
}
