package com.codereview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 代码审查提交请求DTO
 */
@Data
public class ReviewSubmitRequest {
    
    @NotBlank(message = "代码内容不能为空")
    private String codeContent;
    
    /** 文件名（可选） */
    @Size(max = 255, message = "文件名长度不能超过255个字符")
    private String fileName;
    
    /** 班级ID（可选，用于班级私有规则审查） */
    private Long classId;
}
