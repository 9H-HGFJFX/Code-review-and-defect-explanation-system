package com.codereview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求DTO
 */
@Data
public class RegisterRequest {
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度应在3-50个字符之间")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度应不少于6位")
    private String password;
    
    /** 角色：STUDENT/TEACHER（默认STUDENT） */
    private String role = "STUDENT";
    
    /** 邮箱 */
    private String email;
}
