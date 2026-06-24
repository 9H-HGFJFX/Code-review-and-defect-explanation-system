package com.codereview.scheduler;

import com.codereview.common.enums.TaskStatus;
import lombok.Data;

/**
 * 扫描任务信息封装类
 * 用于在任务队列中传递任务信息
 * 
 * @author code-review-team
 */
@Data
public class ScanTaskInfo {
    
    /**
     * 任务ID
     */
    private Long taskId;
    
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 关联的项目ID
     */
    private Long projectId;
    
    /**
     * 文件路径列表（可以是目录或文件）
     */
    private java.util.List<String> filePaths;
    
    /**
     * 提交人ID
     */
    private Long submitterId;
    
    /**
     * 任务优先级（1-10，数字越小优先级越高）
     */
    private Integer priority;
    
    /**
     * 创建时间
     */
    private Long createTime;
    
    /**
     * 任务状态
     */
    private TaskStatus status;
    
    /**
     * 重试次数
     */
    private int retryCount;
    
    /**
     * 最大重试次数
     */
    private int maxRetries;

    /**
     * 创建扫描任务信息
     */
    public static ScanTaskInfo create(Long taskId, String taskName, Long projectId) {
        ScanTaskInfo info = new ScanTaskInfo();
        info.setTaskId(taskId);
        info.setTaskName(taskName);
        info.setProjectId(projectId);
        info.setPriority(5); // 默认优先级
        info.setCreateTime(System.currentTimeMillis());
        info.setStatus(TaskStatus.PENDING);
        info.setRetryCount(0);
        info.setMaxRetries(3);
        return info;
    }

    /**
     * 增加重试次数
     */
    public void incrementRetry() {
        this.retryCount++;
    }

    /**
     * 是否可以重试
     */
    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    /**
     * 序列化任务信息为JSON
     */
    public String toJson() {
        return com.alibaba.fastjson2.JSON.toJSONString(this);
    }

    /**
     * 从JSON反序列化
     */
    public static ScanTaskInfo fromJson(String json) {
        return com.alibaba.fastjson2.JSON.parseObject(json, ScanTaskInfo.class);
    }
}
