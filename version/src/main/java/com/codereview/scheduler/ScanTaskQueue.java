package com.codereview.scheduler;

import com.codereview.common.enums.TaskStatus;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 扫描任务队列管理
 * 基于Redis实现分布式任务队列
 * 
 * 功能：
 * - 任务入队（LPUSH）
 * - 任务出队（BRPOP阻塞式拉取）
 * - 任务状态管理
 * - 并发控制
 * 
 * @author code-review-team
 */
@Component
@Slf4j
public class ScanTaskQueue {

    /**
     * 任务队列键
     */
    @Value("${review.scheduler.queue-name:scan:task:queue}")
    private String queueName;

    /**
     * 任务状态键前缀
     */
    @Value("${review.scheduler.status-key-prefix:scan:task:status:}")
    private String statusKeyPrefix;

    /**
     * 最大并发任务数
     */
    @Value("${review.scheduler.max-concurrent-tasks:10}")
    private int maxConcurrentTasks;

    /**
     * 任务状态过期时间（秒）
     */
    @Value("${review.scheduler.task-status-ttl-seconds:86400}")
    private long taskStatusTtlSeconds;

    /**
     * Redis模板
     */
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 将任务加入队列
     * 
     * @param taskInfo 任务信息
     * @return 是否入队成功
     */
    public boolean enqueue(ScanTaskInfo taskInfo) {
        try {
            String json = taskInfo.toJson();
            // 使用LPUSH将任务加入队列（左侧）
            Long result = redisTemplate.opsForList().leftPush(queueName, json);
            
            log.info("任务入队成功: taskId={}, queueSize={}", 
                taskInfo.getTaskId(), result);
            
            return result != null && result > 0;
            
        } catch (Exception e) {
            log.error("任务入队失败: taskId={}, error={}", 
                taskInfo.getTaskId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 从队列中取出任务（阻塞式）
     * 
     * @param timeout 超时时间（秒）
     * @return 任务信息，如果没有任务则返回null
     */
    public ScanTaskInfo dequeue(long timeout) {
        try {
            // 使用BRPOP阻塞式拉取
            Object taskObj = redisTemplate.opsForList().rightPop(queueName, Duration.ofSeconds(timeout));
            
            if (taskObj == null) {
                return null;
            }
            
            String json = taskObj.toString();
            
            ScanTaskInfo taskInfo = ScanTaskInfo.fromJson(json);
            log.debug("任务出队成功: taskId={}", taskInfo.getTaskId());
            
            return taskInfo;
            
        } catch (Exception e) {
            log.error("任务出队失败: error={}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 非阻塞式取出任务
     * 
     * @return 任务信息，如果没有任务则返回null
     */
    public ScanTaskInfo dequeueNow() {
        try {
            Object taskObj = redisTemplate.opsForList().rightPop(queueName);
            
            if (taskObj == null) {
                return null;
            }
            
            String json = taskObj.toString();
            return ScanTaskInfo.fromJson(json);
            
        } catch (Exception e) {
            log.error("任务出队失败: error={}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 更新任务状态
     * 
     * @param taskId 任务ID
     * @param status 任务状态
     */
    public void updateStatus(Long taskId, TaskStatus status) {
        String key = statusKeyPrefix + taskId;
        
        try {
            Map<String, Object> statusInfo = Map.of(
                "status", status.name(),
                "updateTime", System.currentTimeMillis()
            );
            
            redisTemplate.opsForHash().putAll(key, statusInfo);
            redisTemplate.expire(key, taskStatusTtlSeconds, TimeUnit.SECONDS);
            
            log.debug("任务状态更新: taskId={}, status={}", taskId, status);
            
        } catch (Exception e) {
            log.error("更新任务状态失败: taskId={}, error={}", taskId, e.getMessage(), e);
        }
    }

    /**
     * 获取任务状态
     * 
     * @param taskId 任务ID
     * @return 任务状态
     */
    public TaskStatus getStatus(Long taskId) {
        String key = statusKeyPrefix + taskId;
        
        try {
            Object status = redisTemplate.opsForHash().get(key, "status");
            if (status != null) {
                return TaskStatus.valueOf(status.toString());
            }
        } catch (Exception e) {
            log.error("获取任务状态失败: taskId={}, error={}", taskId, e.getMessage());
        }
        
        return null;
    }

    /**
     * 获取当前队列长度
     * 
     * @return 队列中等待的任务数
     */
    public long getQueueLength() {
        try {
            Long size = redisTemplate.opsForList().size(queueName);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("获取队列长度失败: error={}", e.getMessage());
            return 0;
        }
    }

    /**
     * 检查是否可以执行新任务（并发控制）
     * 
     * @return 是否可以执行新任务
     */
    public boolean canExecuteTask() {
        try {
            String countKey = statusKeyPrefix + "running:count";
            Long count = redisTemplate.opsForValue().increment(countKey);
            
            if (count == null) {
                return true;
            }
            
            if (count > maxConcurrentTasks) {
                // 超过最大并发数，减少计数
                redisTemplate.opsForValue().decrement(countKey);
                return false;
            }
            
            // 设置过期时间
            redisTemplate.expire(countKey, 1, TimeUnit.HOURS);
            return true;
            
        } catch (Exception e) {
            log.error("检查并发控制失败: error={}", e.getMessage());
            return true; // 默认允许执行
        }
    }

    /**
     * 任务执行完成，释放并发计数
     */
    public void releaseTask() {
        try {
            String countKey = statusKeyPrefix + "running:count";
            redisTemplate.opsForValue().decrement(countKey);
        } catch (Exception e) {
            log.error("释放并发计数失败: error={}", e.getMessage());
        }
    }

    /**
     * 获取正在运行的任务数
     * 
     * @return 运行中的任务数
     */
    public long getRunningTaskCount() {
        try {
            String countKey = statusKeyPrefix + "running:count";
            Object count = redisTemplate.opsForValue().get(countKey);
            return count != null ? Long.parseLong(count.toString()) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 获取队列统计信息
     */
    public QueueStats getStats() {
        QueueStats stats = new QueueStats();
        stats.setQueueLength(getQueueLength());
        stats.setRunningCount(getRunningTaskCount());
        stats.setMaxConcurrent(maxConcurrentTasks);
        return stats;
    }

    /**
     * 队列统计信息
     */
    @Data
    public static class QueueStats {
        private long queueLength;
        private long runningCount;
        private int maxConcurrent;
        
        public long getAvailableCapacity() {
            return maxConcurrent - runningCount;
        }
    }
}
