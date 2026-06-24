package com.codereview.engine.detector;

import com.codereview.common.enums.FailureReason;
import lombok.Data;

import java.util.List;

/**
 * 检测结果封装类
 * 包含检测到的问题列表和解析失败的文件信息
 * 
 * @author code-review-team
 */
@Data
public class DetectorResult {
    
    /**
     * 检测到的问题列表
     */
    private List<com.codereview.entity.CodeIssue> issues;
    
    /**
     * 解析失败的文件列表
     */
    private List<FailedFileInfo> failedFiles;
    
    /**
     * 扫描统计信息
     */
    private ScanStats stats;

    /**
     * 创建成功结果
     */
    public static DetectorResult success(List<com.codereview.entity.CodeIssue> issues) {
        DetectorResult result = new DetectorResult();
        result.setIssues(issues);
        result.setFailedFiles(List.of());
        return result;
    }

    /**
     * 创建完整结果（包含失败文件）
     */
    public static DetectorResult complete(List<com.codereview.entity.CodeIssue> issues,
                                          List<FailedFileInfo> failedFiles,
                                          ScanStats stats) {
        DetectorResult result = new DetectorResult();
        result.setIssues(issues);
        result.setFailedFiles(failedFiles);
        result.setStats(stats);
        return result;
    }

    /**
     * 解析失败的文件信息
     */
    @Data
    public static class FailedFileInfo {
        
        /**
         * 文件路径
         */
        private String filePath;
        
        /**
         * 失败原因
         * @see com.codereview.common.enums.FailureReason
         */
        private FailureReason reason;
        
        /**
         * 详细错误信息
         */
        private String detail;
        
        /**
         * 出错的行号（如果有）
         */
        private Integer lineNumber;

        public FailedFileInfo(String filePath, FailureReason reason, String detail) {
            this.filePath = filePath;
            this.reason = reason;
            this.detail = detail;
        }

        public FailedFileInfo(String filePath, FailureReason reason, String detail, Integer lineNumber) {
            this.filePath = filePath;
            this.reason = reason;
            this.detail = detail;
            this.lineNumber = lineNumber;
        }
    }

    /**
     * 扫描统计信息
     */
    @Data
    public static class ScanStats {
        
        /**
         * 总文件数
         */
        private int totalFiles;
        
        /**
         * 成功处理的文件数
         */
        private int processedFiles;
        
        /**
         * 失败的文件数
         */
        private int failedFiles;
        
        /**
         * 检测到的问题总数
         */
        private int totalIssues;
        
        /**
         * 扫描开始时间
         */
        private long startTime;
        
        /**
         * 扫描结束时间
         */
        private long endTime;
        
        /**
         * 扫描耗时（毫秒）
         */
        private long durationMs;

        public ScanStats() {
        }

        public ScanStats(int totalFiles, int processedFiles, int failedFiles) {
            this.totalFiles = totalFiles;
            this.processedFiles = processedFiles;
            this.failedFiles = failedFiles;
        }

        public ScanStats(int totalFiles, int processedFiles, int failedFiles, long startTime, long endTime) {
            this.totalFiles = totalFiles;
            this.processedFiles = processedFiles;
            this.failedFiles = failedFiles;
            this.startTime = startTime;
            this.endTime = endTime;
            this.durationMs = endTime - startTime;
        }
    }
}
