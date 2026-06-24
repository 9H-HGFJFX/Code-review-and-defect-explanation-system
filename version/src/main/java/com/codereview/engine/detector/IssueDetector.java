package com.codereview.engine.detector;

import com.codereview.common.enums.FailureReason;
import com.codereview.engine.parser.*;
import com.codereview.engine.rule.RuleEngine;
import com.codereview.engine.rule.RuleLoader;
import com.codereview.entity.CodeIssue;
import com.codereview.entity.ReviewRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 问题检测器核心类
 * 负责对源代码文件进行缺陷检测
 * 
 * 支持功能：
 * - 单文件和批量文件检测
 * - AST解析异常单文件隔离
 * - 多线程并行处理
 * - 超时控制
 * 
 * @author code-review-team
 */
@Service
@Slf4j
public class IssueDetector {

    /**
     * 解析器工厂
     */
    @Autowired
    private ParserFactory parserFactory;

    /**
     * 规则引擎
     */
    @Autowired
    private RuleEngine ruleEngine;

    /**
     * 规则加载器
     */
    @Autowired
    private RuleLoader ruleLoader;

    /**
     * 单文件解析超时时间（毫秒）
     */
    @Value("${review.engine.parser.parse-timeout-ms:30000}")
    private long parseTimeoutMs;

    /**
     * 最大文件行数
     */
    @Value("${review.engine.parser.max-file-lines:50000}")
    private int maxFileLines;

    /**
     * 线程池
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors(),
        new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "detector-" + counter.getAndIncrement());
            }
        }
    );

    /**
     * 单文件检测
     * 
     * @param file 源代码文件
     * @return 检测结果
     */
    public List<CodeIssue> detect(SourceFile file) {
        if (file == null) {
            log.warn("检测文件为空");
            return new ArrayList<>();
        }
        
        try {
            // 获取适用的规则
            List<ReviewRule> rules = getApplicableRules(file);
            
            // 获取对应语言的解析器
            SourceParser parser = parserFactory.getParser(file.getLanguage());
            if (parser == null) {
                log.warn("未找到语言 {} 对应的解析器", file.getLanguage());
                return new ArrayList<>();
            }
            
            // 解析文件
            ParseResult parseResult;
            try {
                parseResult = parseWithTimeout(parser, file, rules, parseTimeoutMs);
            } catch (TimeoutException e) {
                log.warn("文件解析超时: {}, 超时时间: {}ms", file.getPath(), parseTimeoutMs);
                return new ArrayList<>();
            }
            
            // 执行规则匹配
            List<CodeIssue> issues = ruleEngine.match(file, rules);
            
            log.debug("文件检测完成: {}, 规则数: {}, 问题数: {}", 
                file.getPath(), rules.size(), issues.size());
            
            return issues;
            
        } catch (Exception e) {
            log.error("文件检测失败: {}, 错误: {}", file.getPath(), e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 批量检测
     * 
     * @param files 文件列表
     * @return 包含问题列表、失败文件列表和统计信息的检测结果
     */
    public DetectorResult detectBatch(List<SourceFile> files) {
        if (files == null || files.isEmpty()) {
            log.info("待检测文件列表为空");
            return DetectorResult.complete(List.of(), List.of(), new DetectorResult.ScanStats(0, 0, 0));
        }
        
        long startTime = System.currentTimeMillis();
        List<CodeIssue> allIssues = new ArrayList<>();
        List<DetectorResult.FailedFileInfo> failedFiles = new ArrayList<>();
        int processedCount = 0;
        
        log.info("开始批量检测: 文件总数={}", files.size());
        
        // 加载所有规则
        List<ReviewRule> allRules = ruleLoader.getLoadedRules();
        ruleEngine.loadRules(allRules);
        
        // 逐个文件处理（带隔离）
        for (SourceFile file : files) {
            try {
                // 文件大小检查
                if (file.getFileSize() != null && file.getFileSize() > 10 * 1024 * 1024) {
                    // 文件超过10MB，记录失败
                    failedFiles.add(new DetectorResult.FailedFileInfo(
                        file.getPath(),
                        FailureReason.FILE_TOO_LARGE,
                        "文件大小超过10MB限制: " + file.getFileSize() + " bytes"
                    ));
                    continue;
                }
                
                // 行数检查
                if (file.getLineCount() != null && file.getLineCount() > maxFileLines) {
                    failedFiles.add(new DetectorResult.FailedFileInfo(
                        file.getPath(),
                        FailureReason.PARSE_ERROR,
                        "文件行数超过限制: " + file.getLineCount() + " > " + maxFileLines
                    ));
                    continue;
                }
                
                // 获取适用规则
                List<ReviewRule> applicableRules = filterApplicableRules(file, allRules);
                
                // 获取解析器
                SourceParser parser = parserFactory.getParser(file.getLanguage());
                if (parser == null) {
                    log.warn("未找到语言 {} 的解析器，跳过文件: {}", 
                        file.getLanguage(), file.getPath());
                    continue;
                }
                
                // 带超时的解析
                try {
                    ParseResult parseResult = parseWithTimeout(parser, file, applicableRules, parseTimeoutMs);
                    
                    // 执行规则匹配
                    List<CodeIssue> issues = ruleEngine.match(file, applicableRules);
                    allIssues.addAll(issues);
                    processedCount++;
                    
                } catch (TimeoutException e) {
                    // 解析超时
                    log.warn("文件解析超时: {}", file.getPath());
                    failedFiles.add(new DetectorResult.FailedFileInfo(
                        file.getPath(),
                        FailureReason.TIMEOUT,
                        "解析超时: " + parseTimeoutMs + "ms"
                    ));
                    
                } catch (ParseException e) {
                    // 语法解析错误 - 单文件隔离，不影响其他文件
                    log.error("文件解析失败: {}, 原因: {}", file.getPath(), e.getMessage());
                    failedFiles.add(new DetectorResult.FailedFileInfo(
                        file.getPath(),
                        FailureReason.PARSE_ERROR,
                        e.getMessage(),
                        e.getLineNumber()
                    ));
                    // 继续处理下一个文件，不中断
                }
                
            } catch (Exception e) {
                // 其他异常 - 单文件隔离
                log.error("文件处理异常: {}, 错误: {}", file.getPath(), e.getMessage(), e);
                failedFiles.add(new DetectorResult.FailedFileInfo(
                    file.getPath(),
                    FailureReason.UNKNOWN_ERROR,
                    e.getMessage()
                ));
                // 继续处理下一个文件
            }
        }
        
        long endTime = System.currentTimeMillis();
        
        // 构建统计信息
        DetectorResult.ScanStats stats = new DetectorResult.ScanStats(
            files.size(), processedCount, failedFiles.size(), startTime, endTime
        );
        stats.setTotalIssues(allIssues.size());
        
        log.info("批量检测完成: 总文件={}, 成功={}, 失败={}, 问题数={}, 耗时={}ms",
            files.size(), processedCount, failedFiles.size(), allIssues.size(), 
            endTime - startTime);
        
        return DetectorResult.complete(allIssues, failedFiles, stats);
    }

    /**
     * 带超时的解析
     */
    private ParseResult parseWithTimeout(SourceParser parser, SourceFile file, 
                                           List<ReviewRule> rules, long timeoutMs) 
            throws TimeoutException, ParseException {
        
        Future<ParseResult> future = executorService.submit(() -> 
            parser.parse(file, rules)
        );
        
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new TimeoutException("文件解析超时: " + file.getPath());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ParseException("解析被中断: " + file.getPath(), e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ParseException) {
                throw (ParseException) e.getCause();
            }
            throw new ParseException("解析执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取适用规则
     */
    private List<ReviewRule> getApplicableRules(SourceFile file) {
        return ruleEngine.getRulesByLanguage(file.getLanguage());
    }

    /**
     * 过滤适用规则
     */
    private List<ReviewRule> filterApplicableRules(SourceFile file, List<ReviewRule> rules) {
        if (file.getLanguage() == null) {
            return rules;
        }
        
        String langCode = file.getLanguage().getCode().toLowerCase();
        
        return rules.stream()
            .filter(ReviewRule::getEnabled)
            .filter(r -> {
                if (r.getLanguages() == null || r.getLanguages().isEmpty()) {
                    return true;
                }
                String[] langs = r.getLanguages().toLowerCase().split(",");
                for (String lang : langs) {
                    if (lang.trim().equals(langCode) || lang.trim().equals("all")) {
                        return true;
                    }
                }
                return false;
            })
            .toList();
    }

    /**
     * 线程工厂构建器
     */
    private static class ThreadFactoryBuilder {
        public ThreadFactory setNameFormat(String format) {
            return r -> {
                Thread t = new Thread(r);
                t.setName(String.format(format, t.getId()));
                return t;
            };
        }
    }
}
