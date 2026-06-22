package com.codereview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codereview.common.PageRequest;
import com.codereview.common.PageResponse;
import com.codereview.dto.RuleRequest;
import com.codereview.entity.Rule;
import com.codereview.exception.BusinessException;
import com.codereview.mapper.RuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 规则管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleService extends ServiceImpl<RuleMapper, Rule> {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String RULE_CACHE_KEY = "rules:enabled";
    private static final String RULE_CACHE_PREFIX = "rules:class:";
    
    /**
     * 初始化默认规则
     */
    @PostConstruct
    public void initDefaultRules() {
        if (lambdaQuery().eq(Rule::getEnabled, 1).count() == 0) {
            log.info("初始化默认规则...");
            initSecurityRules();
            initDefectRules();
            initStyleRules();
        }
        
        // 加载规则到缓存
        refreshRuleCache();
    }
    
    /**
     * 初始化安全规则
     */
    private void initSecurityRules() {
        List<Rule> rules = List.of(
            createRule(Rule.CATEGORY_SECURITY, "检测硬编码密码", Rule.PATTERN_REGEX,
                "(password|passwd|pwd|secret)\\s*=\\s*[\"'][^\"']{4,}[\"']",
                Issue.SEVERITY_CRITICAL, "检测到硬编码敏感信息"),
            
            createRule(Rule.CATEGORY_SECURITY, "检测SQL注入风险", Rule.PATTERN_REGEX,
                "Statement\\s*\\.\\s*execute.*\\+",
                Issue.SEVERITY_CRITICAL, "检测到SQL注入风险"),
            
            createRule(Rule.CATEGORY_SECURITY, "检测硬编码API Key", Rule.PATTERN_REGEX,
                "(api_key|apikey|access_key|accesskey)\\s*=\\s*[\"'][^\"']+[\"']",
                Issue.SEVERITY_CRITICAL, "检测到硬编码API密钥")
        );
        
        saveBatch(rules);
    }
    
    /**
     * 初始化缺陷规则
     */
    private void initDefectRules() {
        List<Rule> rules = List.of(
            createRule(Rule.CATEGORY_DEFECT, "检测空指针风险", Rule.PATTERN_REGEX,
                "\\.\\s*toString\\s*\\(\\s*\\)",
                Issue.SEVERITY_WARNING, "可能存在空指针风险"),
            
            createRule(Rule.CATEGORY_DEFECT, "检测未关闭资源", Rule.PATTERN_REGEX,
                "new\\s+File(Input|Output)Stream",
                Issue.SEVERITY_WARNING, "资源可能未正确关闭")
        );
        
        saveBatch(rules);
    }
    
    /**
     * 初始化风格规则
     */
    private void initStyleRules() {
        List<Rule> rules = List.of(
            createRule(Rule.CATEGORY_STYLE, "检测过长方法", Rule.PATTERN_AST,
                null, Issue.SEVERITY_WARNING, "方法过长，建议拆分"),
            
            createRule(Rule.CATEGORY_STYLE, "检测TODO注释", Rule.PATTERN_REGEX,
                "//\\s*TODO|//\\s*FIXME",
                Issue.SEVERITY_SUGGESTION, "存在未完成的TODO注释")
        );
        
        saveBatch(rules);
    }
    
    /**
     * 创建规则对象
     */
    private Rule createRule(String category, String name, String patternType, 
                           String pattern, String severity, String description) {
        Rule rule = new Rule();
        rule.setCategory(category);
        rule.setName(name);
        rule.setPatternType(patternType);
        rule.setPattern(pattern);
        rule.setSeverity(severity);
        rule.setDescription(description);
        rule.setEnabled(1);
        return rule;
    }
    
    /**
     * 获取启用的规则列表
     */
    public List<Rule> getEnabledRules(Long classId) {
        String cacheKey = classId != null ? RULE_CACHE_PREFIX + classId : RULE_CACHE_KEY;
        
        @SuppressWarnings("unchecked")
        List<Rule> cachedRules = (List<Rule>) redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedRules != null) {
            return cachedRules;
        }
        
        // 从数据库加载
        LambdaQueryWrapper<Rule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Rule::getEnabled, 1)
               .and(w -> w.isNull(Rule::getClassId).or().eq(Rule::getClassId, classId));
        
        List<Rule> rules = list(wrapper);
        
        // 缓存
        redisTemplate.opsForValue().set(cacheKey, rules, 1, TimeUnit.HOURS);
        
        return rules;
    }
    
    /**
     * 分页查询规则
     */
    public PageResponse<Rule> getRuleList(PageRequest pageRequest, String category, Integer enabled) {
        Page<Rule> page = new Page<>(pageRequest.getCurrent(), pageRequest.getSize());
        
        LambdaQueryWrapper<Rule> wrapper = new LambdaQueryWrapper<>();
        
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Rule::getCategory, category);
        }
        if (enabled != null) {
            wrapper.eq(Rule::getEnabled, enabled);
        }
        
        wrapper.orderByDesc(Rule::getCreateTime);
        
        Page<Rule> resultPage = page(page, wrapper);
        
        return PageResponse.ok(
            resultPage.getRecords(),
            resultPage.getTotal(),
            resultPage.getCurrent(),
            resultPage.getSize()
        );
    }
    
    /**
     * 创建规则
     */
    @Transactional
    public Rule createRule(RuleRequest request) {
        // 检查规则名称是否重复
        if (lambdaQuery().eq(Rule::getName, request.getName()).exists()) {
            throw new BusinessException(30002, "规则名称已存在");
        }
        
        Rule rule = new Rule();
        rule.setCategory(request.getCategory());
        rule.setName(request.getName());
        rule.setPatternType(request.getPatternType());
        rule.setPattern(request.getPattern());
        rule.setSeverity(request.getSeverity());
        rule.setClassId(request.getClassId());
        rule.setDescription(request.getDescription());
        rule.setSuggestionTemplate(request.getSuggestionTemplate());
        rule.setEnabled(request.getEnabled() != null ? request.getEnabled() : 1);
        
        save(rule);
        
        // 刷新缓存
        refreshRuleCache();
        
        log.info("创建规则成功: {}", rule.getName());
        return rule;
    }
    
    /**
     * 更新规则
     */
    @Transactional
    public Rule updateRule(Long ruleId, RuleRequest request) {
        Rule rule = getById(ruleId);
        if (rule == null) {
            throw new BusinessException(30001, "规则不存在");
        }
        
        // 检查名称是否重复（排除自己）
        if (!rule.getName().equals(request.getName()) 
            && lambdaQuery().eq(Rule::getName, request.getName()).exists()) {
            throw new BusinessException(30002, "规则名称已存在");
        }
        
        rule.setCategory(request.getCategory());
        rule.setName(request.getName());
        rule.setPatternType(request.getPatternType());
        rule.setPattern(request.getPattern());
        rule.setSeverity(request.getSeverity());
        rule.setClassId(request.getClassId());
        rule.setDescription(request.getDescription());
        rule.setSuggestionTemplate(request.getSuggestionTemplate());
        if (request.getEnabled() != null) {
            rule.setEnabled(request.getEnabled());
        }
        
        updateById(rule);
        
        // 刷新缓存（热更新）
        refreshRuleCache();
        
        log.info("更新规则成功，热更新已触发: {}", rule.getName());
        return rule;
    }
    
    /**
     * 启用/禁用规则
     */
    @Transactional
    public void toggleRule(Long ruleId, boolean enabled) {
        Rule rule = getById(ruleId);
        if (rule == null) {
            throw new BusinessException(30001, "规则不存在");
        }
        
        rule.setEnabled(enabled ? 1 : 0);
        updateById(rule);
        
        // 刷新缓存
        refreshRuleCache();
        
        log.info("规则{}状态更新: enabled={}", rule.getName(), enabled);
    }
    
    /**
     * 删除规则
     */
    @Transactional
    public void deleteRule(Long ruleId) {
        if (!removeById(ruleId)) {
            throw new BusinessException(30001, "规则不存在");
        }
        
        // 刷新缓存
        refreshRuleCache();
        
        log.info("删除规则: id={}", ruleId);
    }
    
    /**
     * 刷新规则缓存（热更新）
     */
    public void refreshRuleCache() {
        // 清除所有规则缓存
        redisTemplate.delete(RULE_CACHE_KEY);
        
        // 清除班级规则缓存
        redisTemplate.delete(redisTemplate.keys(RULE_CACHE_PREFIX + "*"));
        
        // 重新加载
        List<Rule> globalRules = lambdaQuery()
                .eq(Rule::getEnabled, 1)
                .isNull(Rule::getClassId)
                .list();
        
        redisTemplate.opsForValue().set(RULE_CACHE_KEY, globalRules, 1, TimeUnit.HOURS);
        
        log.info("规则缓存已刷新");
    }
    
    /**
     * 获取规则统计
     */
    public Object getRuleStatistics() {
        long total = count();
        long enabled = lambdaQuery().eq(Rule::getEnabled, 1).count();
        long security = lambdaQuery().eq(Rule::getCategory, Rule.CATEGORY_SECURITY).count();
        long defect = lambdaQuery().eq(Rule::getCategory, Rule.CATEGORY_DEFECT).count();
        long style = lambdaQuery().eq(Rule::getCategory, Rule.CATEGORY_STYLE).count();
        
        return new Object() {
            public final long total = total;
            public final long enabled = enabled;
            public final long security = security;
            public final long defect = defect;
            public final long style = style;
        };
    }
}
