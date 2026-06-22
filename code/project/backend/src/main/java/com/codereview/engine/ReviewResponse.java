package com.codereview.engine;

import com.codereview.entity.Issue;
import com.codereview.entity.Rule;
import lombok.Data;
import java.util.List;

/**
 * 代码审查结果封装
 */
@Data
public class ReviewResponse {
    
    /** 是否成功 */
    private boolean success;
    
    /** 错误信息 */
    private String errorMessage;
    
    /** 检测到的问题列表 */
    private List<Issue> issues;
    
    /** 原始代码 */
    private String originalCode;
    
    /** 代码行数 */
    private int lineCount;
    
    public static ReviewResponse success(List<Issue> issues, String originalCode, int lineCount) {
        ReviewResponse response = new ReviewResponse();
        response.setSuccess(true);
        response.setIssues(issues);
        response.setOriginalCode(originalCode);
        response.setLineCount(lineCount);
        return response;
    }
    
    public static ReviewResponse error(String message) {
        ReviewResponse response = new ReviewResponse();
        response.setSuccess(false);
        response.setErrorMessage(message);
        response.setIssues(List.of());
        return response;
    }
}
