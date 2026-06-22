package com.codereview.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

/**
 * 班级成员添加请求DTO
 */
@Data
public class ClassMemberRequest {
    
    @NotEmpty(message = "用户ID列表不能为空")
    private List<Long> userIds;
}
