package com.codeaudit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codeaudit.common.exception.BizException;
import com.codeaudit.common.exception.ForbiddenException;
import com.codeaudit.dto.RuleSaveReq;
import com.codeaudit.entity.Rule;
import com.codeaudit.repository.RuleRepository;
import com.codeaudit.service.RuleService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 规则服务 - 概要设计 3.2.2
 * 内置规则 + 内存缓存 + 热更新（Redis Pub/Sub 预留）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleServiceImpl implements RuleService {

    /** Redis 缓存 key */
    public static final String CACHE_KEY = "code-audit:rules:enabled";
    /** 热更新消息频道 */
    public static final String CHANNEL_RULE_UPDATED = "code-audit:rule:updated";

    private final RuleRepository ruleRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ApplicationEventPublisher publisher;

    /** 内存中启用的规则（兜底用，Redis 不可用时直接走这里） */
    private final AtomicReference<List<Rule>> memoryCache = new AtomicReference<>(Collections.emptyList());

    @PostConstruct
    public void init() {
        refreshCache();
    }

    @Override
    public List<Rule> listEnabled() {
        // 1) 优先从 Redis 读取
        try {
            Object cached = redisTemplate.opsForValue().get(CACHE_KEY);
            if (cached instanceof List<?> list && !list.isEmpty()) {
                @SuppressWarnings("unchecked")
                List<Rule> rules = (List<Rule>) list;
                return rules;
            }
        } catch (Exception e) {
            log.warn("[RULE] Redis 读取失败，降级走内存缓存: {}", e.getMessage());
        }
        // 2) 内存兜底
        return memoryCache.get();
    }

    @Override
    public List<Rule> listAll(int current, int size, String category) {
        LambdaQueryWrapper<Rule> qw = new LambdaQueryWrapper<>();
        if (category != null && !category.isBlank()) qw.eq(Rule::getCategory, category);
        qw.orderByAsc(Rule::getCategory).orderByAsc(Rule::getId);
        return ruleRepository.selectPage(new Page<>(current, size), qw).getRecords();
    }

    @Override
    public long countAll(String category) {
        LambdaQueryWrapper<Rule> qw = new LambdaQueryWrapper<>();
        if (category != null && !category.isBlank()) qw.eq(Rule::getCategory, category);
        return ruleRepository.selectCount(qw);
    }

    @Override
    public Rule getById(Long id) {
        return ruleRepository.selectById(id);
    }

    @Override
    @Transactional
    public Long add(RuleSaveReq req, String currentRole) {
        requireTeacherOrAdmin(currentRole);
        // 唯一性
        Long exists = ruleRepository.selectCount(new LambdaQueryWrapper<Rule>().eq(Rule::getName, req.getName()));
        if (exists > 0) throw new BizException("规则名称已存在");
        Long codeExists = ruleRepository.selectCount(new LambdaQueryWrapper<Rule>().eq(Rule::getCode, req.getCode()));
        if (codeExists > 0) throw new BizException("规则编码已存在");

        Rule r = new Rule();
        copy(req, r);
        r.setIsBuiltin(0);
        r.setCreateTime(LocalDateTime.now());
        r.setUpdateTime(LocalDateTime.now());
        ruleRepository.insert(r);
        refreshCache();
        log.info("[RULE] add id={} name={}", r.getId(), r.getName());
        return r.getId();
    }

    @Override
    @Transactional
    public void update(Long id, RuleSaveReq req, String currentRole) {
        requireTeacherOrAdmin(currentRole);
        Rule r = ruleRepository.selectById(id);
        if (r == null) throw new BizException("规则不存在");
        if (r.getIsBuiltin() != null && r.getIsBuiltin() == 1) {
            // 内置规则允许修改描述/严重级别/建议模板，不允许改 code 与 category
            r.setName(req.getName());
            r.setSeverity(req.getSeverity());
            r.setDescription(req.getDescription());
            r.setSuggestionTemplate(req.getSuggestionTemplate());
            r.setEnabled(req.getEnabled());
            r.setUpdateTime(LocalDateTime.now());
        } else {
            copy(req, r);
            r.setUpdateTime(LocalDateTime.now());
        }
        ruleRepository.updateById(r);
        refreshCache();
        log.info("[RULE] update id={} name={}", r.getId(), r.getName());
    }

    @Override
    @Transactional
    public void delete(Long id, String currentRole) {
        requireTeacherOrAdmin(currentRole);
        Rule r = ruleRepository.selectById(id);
        if (r == null) throw new BizException("规则不存在");
        if (r.getIsBuiltin() != null && r.getIsBuiltin() == 1) {
            throw new BizException("内置规则不可删除");
        }
        ruleRepository.deleteById(id);
        refreshCache();
        log.info("[RULE] delete id={}", id);
    }

    @Override
    @Transactional
    public void toggleEnabled(Long id, Integer enabled, String currentRole) {
        requireTeacherOrAdmin(currentRole);
        Rule r = ruleRepository.selectById(id);
        if (r == null) throw new BizException("规则不存在");
        r.setEnabled(enabled);
        r.setUpdateTime(LocalDateTime.now());
        ruleRepository.updateById(r);
        refreshCache();
    }

    /** 刷新缓存 - 写库后调用，热更新主流程 */
    @Override
    public void refreshCache() {
        List<Rule> rules = ruleRepository.selectList(
                new LambdaQueryWrapper<Rule>().eq(Rule::getEnabled, 1)
        );
        memoryCache.set(rules);
        try {
            redisTemplate.delete(CACHE_KEY);
            redisTemplate.opsForValue().set(CACHE_KEY, rules);
            // 发布消息
            redisTemplate.convertAndSend(CHANNEL_RULE_UPDATED, "refresh");
            log.info("[RULE] 缓存已刷新，启用规则 {} 条", rules.size());
        } catch (Exception e) {
            log.warn("[RULE] Redis 刷新失败，仅内存缓存生效: {}", e.getMessage());
        }
    }

    private void copy(RuleSaveReq req, Rule r) {
        r.setName(req.getName());
        r.setCode(req.getCode());
        r.setCategory(req.getCategory());
        r.setSeverity(req.getSeverity());
        r.setPatternType(req.getPatternType());
        r.setDescription(req.getDescription());
        r.setSuggestionTemplate(req.getSuggestionTemplate());
        r.setExecutorBean(req.getExecutorBean());
        r.setClassId(req.getClassId());
        r.setEnabled(req.getEnabled() == null ? 1 : req.getEnabled());
    }

    private void requireTeacherOrAdmin(String role) {
        if (!"TEACHER".equals(role) && !"ADMIN".equals(role)) {
            throw new ForbiddenException("仅教师/管理员可操作规则");
        }
    }
}
