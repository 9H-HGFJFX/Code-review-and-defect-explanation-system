package com.codereview.scheduler;

import com.codereview.common.enums.ProgrammingLanguage;
import com.codereview.common.enums.TaskStatus;
import com.codereview.engine.detector.DetectorResult;
import com.codereview.engine.parser.SourceFile;
import com.codereview.engine.rule.RuleLoader;
import com.codereview.entity.CodeIssue;
import com.codereview.entity.ReviewTask;
import com.codereview.entity.SubmissionFile;
import com.codereview.repository.CodeIssueMapper;
import com.codereview.repository.ReviewTaskMapper;
import com.codereview.repository.SubmissionFileMapper;
import com.codereview.service.IssueDetectorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 扫描任务调度器
 * 负责管理代码审查任务的调度和执行
 * 
 * 功能：
 * - 基于Redis队列的分布式任务调度
 * - 并发控制（最多10个并行扫描任务）
 * - 超时保护（单文件30秒超时）
 * - 失败重试（最多3次，指数退避）
 * - AST解析异常单文件隔离
 * 
 * @author code-review-team
 */
@Service
@Slf4j
public class ScanScheduler {

    /**
     * 最大并发扫描任务数
     */
    @Value("${review.scheduler.max-concurrent-tasks:10}")
    private int maxConcurrentTasks;

    /**
     * 重试最大次数
     */
    @Value("${review.scheduler.retry.max-attempts:3}")
    private int maxRetryAttempts;

    /**
     * 指数退避乘数
     */
    @Value("${review.scheduler.retry.backoff-multiplier:2}")
    private int backoffMultiplier;

    /**
     * 初始延迟（毫秒）
     */
    @Value("${review.scheduler.retry.initial-delay-ms:1000}")
    private long initialDelayMs;

    /**
     * 最大延迟（毫秒）
     */
    @Value("${review.scheduler.retry.max-delay-ms:30000}")
    private long maxDelayMs;

    /**
     * 单文件解析超时（毫秒）
     */
    @Value("${review.engine.parser.parse-timeout-ms:30000}")
    private long parseTimeoutMs;

    /**
     * 任务队列
     */
    @Autowired
    private ScanTaskQueue taskQueue;

    /**
     * 规则加载器
     */
    @Autowired
    private RuleLoader ruleLoader;

    /**
     * 任务Mapper
     */
    @Autowired
    private ReviewTaskMapper taskMapper;

    /**
     * 问题Mapper
     */
    @Autowired
    private CodeIssueMapper issueMapper;

    /**
     * 文件Mapper
     */
    @Autowired
    private SubmissionFileMapper fileMapper;

    /**
     * 问题检测服务
     */
    @Autowired
    private IssueDetectorService issueDetectorService;

    /**
     * 线程池
     */
    private ExecutorService executorService;

    /**
     * 运行标记
     */
    private final AtomicBoolean running = new AtomicBoolean(true);

    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        // 创建线程池
        executorService = new ThreadPoolExecutor(
            maxConcurrentTasks,
            maxConcurrentTasks,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(maxConcurrentTasks * 2),
            r -> {
                Thread t = new Thread(r);
                t.setName("scan-scheduler-" + t.getId());
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        log.info("扫描调度器初始化完成: maxConcurrentTasks={}", maxConcurrentTasks);
    }

    /**
     * 销毁
     */
    @PreDestroy
    public void destroy() {
        running.set(false);
        if (executorService != null) {
            executorService.shutdown();
        }
        log.info("扫描调度器已关闭");
    }

    /**
     * 提交扫描任务到队列
     * 
     * @param taskId 任务ID
     * @return 是否提交成功
     */
    public boolean submitScanTask(Long taskId) {
        try {
            // 获取任务信息
            ReviewTask task = taskMapper.selectById(taskId);
            if (task == null) {
                log.error("任务不存在: taskId={}", taskId);
                return false;
            }
            
            // 创建任务信息
            ScanTaskInfo taskInfo = ScanTaskInfo.create(taskId, task.getTitle(), task.getProjectId());
            
            // 获取任务关联的文件
            List<SubmissionFile> files = fileMapper.selectByTaskId(taskId);
            List<String> filePaths = new ArrayList<>();
            for (SubmissionFile file : files) {
                filePaths.add(file.getContentPath());
            }
            taskInfo.setFilePaths(filePaths);
            
            // 入队
            boolean result = taskQueue.enqueue(taskInfo);
            
            if (result) {
                // 更新任务状态为PENDING
                updateTaskStatus(taskId, TaskStatus.PENDING);
                log.info("扫描任务已提交到队列: taskId={}", taskId);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("提交扫描任务失败: taskId={}, error={}", taskId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 执行扫描任务
     * 
     * @param taskId 任务ID
     * @return 扫描结果
     */
    public DetectorResult executeScan(Long taskId) {
        log.info("开始执行扫描任务: taskId={}", taskId);
        
        long startTime = System.currentTimeMillis();
        DetectorResult result = null;
        
        try {
            // 检查并发控制
            if (!taskQueue.canExecuteTask()) {
                log.warn("并发任务数已满，任务需要等待: taskId={}", taskId);
                return null;
            }
            
            // 获取任务信息
            ReviewTask task = taskMapper.selectById(taskId);
            if (task == null) {
                log.error("任务不存在: taskId={}", taskId);
                return null;
            }
            
            // 更新状态为RUNNING
            updateTaskStatus(taskId, TaskStatus.RUNNING);
            
            // 获取文件列表
            List<SubmissionFile> submissionFiles = fileMapper.selectByTaskId(taskId);
            if (submissionFiles.isEmpty()) {
                log.warn("任务没有关联的文件: taskId={}", taskId);
                updateTaskStatus(taskId, TaskStatus.COMPLETED);
                return DetectorResult.complete(List.of(), List.of(), new DetectorResult.ScanStats(0, 0, 0, startTime, System.currentTimeMillis()));
            }
            
            // 转换为SourceFile列表
            List<SourceFile> sourceFiles = convertToSourceFiles(submissionFiles);
            
            // 执行批量检测（带异常隔离）
            result = issueDetectorService.detectBatch(sourceFiles, taskId);
            
            // 处理检测结果
            processResult(taskId, result);
            
            // 计算执行时长
            long duration = System.currentTimeMillis() - startTime;
            
            // 根据结果更新任务状态
            if (result.getFailedFiles().isEmpty()) {
                updateTaskStatus(taskId, TaskStatus.COMPLETED);
            } else if (result.getStats().getProcessedFiles() > 0) {
                updateTaskStatus(taskId, TaskStatus.PARTIAL_COMPLETED);
            } else {
                updateTaskStatus(taskId, TaskStatus.FAILED);
            }
            
            log.info("扫描任务执行完成: taskId={}, 耗时={}ms, 问题数={}, 失败文件数={}", 
                taskId, duration, result.getIssues().size(), result.getFailedFiles().size());
            
            return result;
            
        } catch (Exception e) {
            log.error("扫描任务执行失败: taskId={}, error={}", taskId, e.getMessage(), e);
            updateTaskStatus(taskId, TaskStatus.FAILED);
            return result;
            
        } finally {
            // 释放并发计数
            taskQueue.releaseTask();
        }
    }

    /**
     * 带重试的执行扫描
     * 
     * @param taskId 任务ID
     * @return 扫描结果
     */
    public DetectorResult executeScanWithRetry(Long taskId) {
        int attempt = 0;
        long delay = initialDelayMs;
        
        while (attempt < maxRetryAttempts) {
            try {
                DetectorResult result = executeScan(taskId);
                if (result != null) {
                    return result;
                }
            } catch (Exception e) {
                log.warn("扫描任务执行异常（尝试 {}/{}）: taskId={}, error={}", 
                    attempt + 1, maxRetryAttempts, taskId, e.getMessage());
            }
            
            attempt++;
            if (attempt < maxRetryAttempts) {
                log.info("等待 {}ms 后重试（尝试 {}/{}）", delay, attempt + 1, maxRetryAttempts);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                // 指数退避
                delay = Math.min(delay * backoffMultiplier, maxDelayMs);
            }
        }
        
        log.error("扫描任务执行失败，已达到最大重试次数: taskId={}, attempts={}", 
            taskId, maxRetryAttempts);
        updateTaskStatus(taskId, TaskStatus.FAILED);
        
        return null;
    }

    /**
     * 定时拉取任务（Worker角色）
     * 每秒检查一次任务队列
     */
    @Scheduled(fixedDelay = 1000)
    public void pollTask() {
        if (!running.get()) {
            return;
        }
        
        try {
            // 阻塞式获取任务（超时5秒）
            ScanTaskInfo taskInfo = taskQueue.dequeue(5);
            
            if (taskInfo != null) {
                log.info("从队列中获取任务: taskId={}", taskInfo.getTaskId());
                
                // 提交到线程池执行
                executorService.submit(() -> {
                    try {
                        executeScanWithRetry(taskInfo.getTaskId());
                    } catch (Exception e) {
                        log.error("执行扫描任务失败: taskId={}, error={}", 
                            taskInfo.getTaskId(), e.getMessage(), e);
                    }
                });
            }
            
        } catch (Exception e) {
            log.error("拉取任务失败: error={}", e.getMessage(), e);
        }
    }

    /**
     * 转换文件列表
     */
    private List<SourceFile> convertToSourceFiles(List<SubmissionFile> submissionFiles) {
        List<SourceFile> sourceFiles = new ArrayList<>();
        
        for (SubmissionFile sFile : submissionFiles) {
            try {
                // 读取文件内容
                Path path = Paths.get(sFile.getContentPath());
                if (!Files.exists(path)) {
                    log.warn("文件不存在: {}", sFile.getContentPath());
                    continue;
                }
                
                String content = Files.readString(path, StandardCharsets.UTF_8);
                
                SourceFile sourceFile = SourceFile.builder()
                    .path(sFile.getFilePath())
                    .content(content)
                    .fileName(sFile.getFileName())
                    .extension(sFile.getExtension())
                    .language(ProgrammingLanguage.fromName(sFile.getLanguage()))
                    .fileSize(sFile.getFileSize())
                    .lineCount(sFile.getLineCount())
                    .build();
                
                sourceFiles.add(sourceFile);
                
            } catch (Exception e) {
                log.error("读取文件失败: {}, error={}", sFile.getContentPath(), e.getMessage());
            }
        }
        
        return sourceFiles;
    }

    /**
     * 处理检测结果
     */
    private void processResult(Long taskId, DetectorResult result) {
        if (result == null || result.getIssues().isEmpty()) {
            return;
        }
        
        // 保存问题到数据库
        for (CodeIssue issue : result.getIssues()) {
            issue.setTaskId(taskId);
            issueMapper.insert(issue);
        }
        
        // 更新文件状态
        for (DetectorResult.FailedFileInfo failedFile : result.getFailedFiles()) {
            // 查找对应的submission_file记录
            List<SubmissionFile> files = fileMapper.selectByTaskId(taskId);
            for (SubmissionFile file : files) {
                if (file.getFilePath().equals(failedFile.getFilePath())) {
                    file.setStatus("FAILED");
                    file.setFailReason(failedFile.getDetail());
                    fileMapper.updateById(file);
                    break;
                }
            }
        }
    }

    /**
     * 更新任务状态
     */
    private void updateTaskStatus(Long taskId, TaskStatus status) {
        try {
            ReviewTask task = taskMapper.selectById(taskId);
            if (task != null) {
                task.setStatus(status.getCode());
                
                if (status == TaskStatus.RUNNING) {
                    task.setStartTime(LocalDateTime.now());
                } else if (status == TaskStatus.COMPLETED || status == TaskStatus.FAILED 
                        || status == TaskStatus.PARTIAL_COMPLETED) {
                    task.setEndTime(LocalDateTime.now());
                    // 计算执行时长
                    if (task.getStartTime() != null) {
                        long durationSeconds = java.time.Duration.between(
                            task.getStartTime(), task.getEndTime()).getSeconds();
                        task.setDurationSeconds(durationSeconds);
                    }
                }
                
                taskMapper.updateById(task);
                
                // 同时更新Redis状态
                taskQueue.updateStatus(taskId, status);
            }
        } catch (Exception e) {
            log.error("更新任务状态失败: taskId={}, status={}, error={}", 
                taskId, status, e.getMessage());
        }
    }

    /**
     * 取消任务
     */
    public boolean cancelTask(Long taskId) {
        try {
            TaskStatus currentStatus = taskQueue.getStatus(taskId);
            
            if (currentStatus == TaskStatus.RUNNING) {
                // 运行中的任务需要中断
                log.warn("任务正在运行，无法直接取消: taskId={}", taskId);
                return false;
            }
            
            updateTaskStatus(taskId, TaskStatus.CANCELLED);
            log.info("任务已取消: taskId={}", taskId);
            return true;
            
        } catch (Exception e) {
            log.error("取消任务失败: taskId={}, error={}", taskId, e.getMessage());
            return false;
        }
    }

    /**
     * 获取任务队列统计
     */
    public ScanTaskQueue.QueueStats getQueueStats() {
        return taskQueue.getStats();
    }
}
