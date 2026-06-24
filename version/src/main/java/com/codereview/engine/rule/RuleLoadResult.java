package com.codereview.engine.rule;

import com.codereview.common.enums.FailMode;
import com.codereview.entity.ReviewRule;
import lombok.Data;

import java.util.List;

/**
 * 规则加载结果封装类
 * 包含加载成功和失败的规则信息
 * 
 * @author code-review-team
 */
@Data
public class RuleLoadResult {
    
    /**
     * 加载成功的规则列表
     */
    private List<ReviewRule> loadedRules;
    
    /**
     * 加载失败的规则信息列表
     */
    private List<FailedRuleInfo> failedRules;
    
    /**
     * 加载成功的规则数量
     */
    private int successCount;
    
    /**
     * 加载失败的规则数量
     */
    private int failureCount;
    
    /**
     * 总共尝试加载的规则数量
     */
    private int totalCount;
    
    /**
     * 是否全部加载成功
     */
    private boolean allLoaded;
    
    /**
     * 加载失败模式
     */
    private FailMode failMode;
    
    /**
     * 汇总统计信息
     */
    public void summarize() {
        this.totalCount = (loadedRules != null ? loadedRules.size() : 0) 
                         + (failedRules != null ? failedRules.size() : 0);
        this.successCount = loadedRules != null ? loadedRules.size() : 0;
        this.failureCount = failedRules != null ? failedRules.size() : 0;
        this.allLoaded = failureCount == 0;
    }

    /**
     * 加载失败的规则信息内部类
     */
    @Data
    public static class FailedRuleInfo {
        
        /**
         * 规则文件名称
         */
        private String fileName;
        
        /**
         * 失败原因
         */
        private String reason;
        
        /**
         * 详细错误信息
         */
        private String detail;
        
        /**
         * 异常堆栈摘要
         */
        private String stackTrace;

        public FailedRuleInfo(String fileName, String reason, Throwable throwable) {
            this.fileName = fileName;
            this.reason = reason;
            if (throwable != null) {
                this.detail = throwable.getMessage();
                // 截取堆栈前3行
                StringBuilder sb = new StringBuilder();
                for (StackTraceElement element : throwable.getStackTrace()) {
                    if (sb.length() > 200) break;
                    sb.append(element.toString()).append("\n");
                }
                this.stackTrace = sb.toString();
            }
        }

        public FailedRuleInfo(String fileName, String reason, String detail) {
            this.fileName = fileName;
            this.reason = reason;
            this.detail = detail;
        }
    }
}
