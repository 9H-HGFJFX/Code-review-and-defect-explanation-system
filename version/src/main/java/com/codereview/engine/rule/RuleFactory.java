package com.codereview.engine.rule;

import com.codereview.common.enums.RuleCategory;
import com.codereview.entity.ReviewRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 规则工厂类
 * 根据规则类别创建不同的检测器
 * 
 * @author code-review-team
 */
@Component
@Slf4j
public class RuleFactory {

    /**
     * 检测器注册表
     */
    private final Map<RuleCategory, RuleDetector> detectors = new ConcurrentHashMap<>();

    /**
     * 构造函数 - 注册默认检测器
     */
    public RuleFactory() {
        // 注册内置检测器
        registerDetector(RuleCategory.SECURITY, new SecurityRuleDetector());
        registerDetector(RuleCategory.STYLE, new StyleRuleDetector());
        registerDetector(RuleCategory.PERFORMANCE, new PerformanceRuleDetector());
        registerDetector(RuleCategory.BEST_PRACTICE, new BestPracticeRuleDetector());
        registerDetector(RuleCategory.CORRECTNESS, new CorrectnessRuleDetector());
        
        log.info("规则工厂初始化完成，已注册检测器数量: {}", detectors.size());
    }

    /**
     * 注册检测器
     * 
     * @param category 规则类别
     * @param detector 对应的检测器
     */
    public void registerDetector(RuleCategory category, RuleDetector detector) {
        detectors.put(category, detector);
        log.debug("注册检测器: {} -> {}", category, detector.getClass().getSimpleName());
    }

    /**
     * 获取指定类别的检测器
     * 
     * @param category 规则类别
     * @return 对应的检测器
     */
    public RuleDetector getDetector(RuleCategory category) {
        RuleDetector detector = detectors.get(category);
        if (detector == null) {
            log.warn("未找到类别 {} 对应的检测器，返回默认检测器", category);
            return new DefaultRuleDetector();
        }
        return detector;
    }

    /**
     * 根据规则列表创建匹配的检测器
     * 
     * @param rules 规则列表
     * @return 检测器列表
     */
    public List<RuleDetector> getDetectors(List<ReviewRule> rules) {
        // 根据规则的类别返回对应的检测器
        return null; // 简化实现
    }

    /**
     * 获取所有已注册的检测器
     */
    public Map<RuleCategory, RuleDetector> getAllDetectors() {
        return Map.copyOf(detectors);
    }
}
