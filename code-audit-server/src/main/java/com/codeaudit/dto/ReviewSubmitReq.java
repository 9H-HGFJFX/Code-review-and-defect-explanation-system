package com.codeaudit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewSubmitReq {

    @NotBlank(message = "代码内容不能为空")
    @Size(max = 500_000, message = "单次提交代码不能超过 500KB")
    private String code;

    private String fileName;
}
