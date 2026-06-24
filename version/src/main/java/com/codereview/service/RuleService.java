package com.codereview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.codereview.common.enums.RuleCategory;
import com.codereview.entity.ReviewRule;
import com.codereview.engine.rule.RuleEngine;
import com.codereview.engine.rule.RuleLoader;
import com.codereview.repository.ReviewRuleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 规则服务实现
 * 提供规则管理功能
 * 
 * @author code-review-team
 */
@Service
@Slf4j
public class RuleService implements RuleServiceInterface {

    @Autowired
    private ReviewRuleMapper ruleMapper;

    @Autowired
    private RuleLoader ruleLoader;

    @Autowired
    private RuleEngine ruleEngine;

    /**
     * 获取所有启用的规则
     * 
     * @return 规则列表
     */
    public List<ReviewRule> listEnabledRules() {
        return ruleMapper.selectAllEnabled();
    }

    /**
     * 根据类别查询规则
     * 
     * @param category 规则类别
     * @return 规则列表
     */
    public List<ReviewRule> listByCategory(RuleCategory category) {
        return ruleMapper.selectByCategory(category);
    }

    /**
     * 根据语言查询规则
     * 
     * @param language 编程语言
     * @return 规则列表
     */
    public List<ReviewRule> listByLanguage(String language) {
        return ruleMapper.selectByLanguage(language);
    }

    /**
     * 创建规则
     * 
     * @param rule 规则实体
     * @param creatorId 创建人ID
     * @return 是否创建成功
     */
    @Transactional
    public boolean createRule(ReviewRule rule, Long creatorId) {
        try {
            // 检查规则ID是否已存在
            ReviewRule existingRule = ruleMapper.selectByRuleId(rule.getRuleId());
            if (existingRule != null) {
                log.warn("规则ID已存在: ruleId={}", rule.getRuleId());
                return false;
            }
            
            rule.setCreatorId(creatorId);
            rule.setCreateTime(LocalDateTime.now());
            rule.setUpdateTime(LocalDateTime.now());
            
            if (rule.getEnabled() == null) {
                rule.setEnabled(true);
            }
            
            if (rule.getPriority() == null) {
                rule.setPriority(100);
            }
            
            ruleMapper.insert(rule);
            
            // 重新加载规则到引擎
            reloadRules();
            
            log.info("创建规则成功: ruleId={}, name={}", rule.getRuleId(), rule.getName());
            return true;
            
        } catch (Exception e) {
            log.error("创建规则失败: ruleId={}, error={}", rule.getRuleId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 更新规则
     * 
     * @param id 规则ID
     * @param rule 规则实体
     * @return 是否更新成功
     */
    @Transactional
    public boolean updateRule(Long id, ReviewRule rule) {
        try {
            ReviewRule existingRule = ruleMapper.selectById(id);
            if (existingRule == null) {
                log.warn("规则不存在: id={}", id);
                return false;
            }
            
            // 更新字段
            existingRule.setName(rule.getName());
            existingRule.setDescription(rule.getDescription());
            existingRule.setCategory(rule.getCategory());
            existingRule.setSeverity(rule.getSeverity());
            existingRule.setLanguages(rule.getLanguages());
            existingRule.setConfig(rule.getConfig());
            existingRule.setPattern(rule.getPattern());
            existingRule.setPriority(rule.getPriority());
            existingRule.setUpdateTime(LocalDateTime.now());
            
            ruleMapper.updateById(existingRule);
            
            // 重新加载规则到引擎
            reloadRules();
            
            log.info("更新规则成功: id={}", id);
            return true;
            
        } catch (Exception e) {
            log.error("更新规则失败: id={}, error={}", id, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 启用/禁用规则
     * 
     * @param id 规则ID
     * @param enabled 是否启用
     * @return 是否更新成功
     */
    @Transactional
    public boolean toggleRule(Long id, boolean enabled) {
        try {
            LambdaUpdateWrapper<ReviewRule> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(ReviewRule::getId, id)
                .set(ReviewRule::getEnabled, enabled)
                .set(ReviewRule::getUpdateTime, LocalDateTime.now());
            
            int result = ruleMapper.update(null, updateWrapper);
            
            if (result > 0) {
                // 重新加载规则到引擎
                reloadRules();
                log.info("规则状态更新: id={}, enabled={}", id, enabled);
                return true;
            }
            
        } catch (Exception e) {
            log.error("切换规则状态失败: id={}, error={}", id, e.getMessage(), e);
        }
        return false;
    }

    /**
     * 删除规则
     * 
     * @param id 规则ID
     * @return 是否删除成功
     */
    @Transactional
    public boolean deleteRule(Long id) {
        try {
            ReviewRule rule = ruleMapper.selectById(id);
            if (rule != null && rule.getBuiltIn()) {
                // 内置规则不允许删除
                log.warn("内置规则不允许删除: id={}", id);
                return false;
            }
            
            int result = ruleMapper.deleteById(id);
            
            if (result > 0) {
                // 重新加载规则到引擎
                reloadRules();
                log.info("删除规则: id={}", id);
                return true;
            }
            
        } catch (Exception e) {
            log.error("删除规则失败: id={}, error={}", id, e.getMessage(), e);
        }
        return false;
    }

    /**
     * 分页获取规则列表（Controller调用）
     */
    public com.codereview.common.result.PageResult<com.codereview.vo.RuleVO> getRuleList(
            com.codereview.dto.PageRequest pageRequest, Boolean enabled, String category) {
        return new com.codereview.common.result.PageResult<>(
                new java.util.ArrayList<>(),
                com.codereview.common.result.PageResult.Pagination.of(pageRequest.getPage(), pageRequest.getPageSize(), 0));
    }

    /**
     * 创建规则（Controller调用）
     */
    public Long createRule(com.codereview.dto.CreateRuleRequest request, Long creatorId) {
        com.codereview.entity.ReviewRule rule = com.codereview.entity.ReviewRule.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(com.codereview.common.enums.RuleCategory.fromName(request.getCategory()))
                .severity(request.getSeverity())
                .pattern(request.getPattern())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .creatorId(creatorId)
                .createTime(java.time.LocalDateTime.now())
                .updateTime(java.time.LocalDateTime.now())
                .build();
        ruleMapper.insert(rule);
        return rule.getId();
    }

    /**
     * 更新规则（Controller调用）
     */
    public void updateRule(Long id, com.codereview.dto.UpdateRuleRequest request, Long operator) {
        com.codereview.entity.ReviewRule rule = ruleMapper.selectById(id);
        if (rule != null) {
            if (request.getName() != null) rule.setName(request.getName());
            if (request.getDescription() != null) rule.setDescription(request.getDescription());
            if (request.getPattern() != null) rule.setPattern(request.getPattern());
            if (request.getSeverity() != null) rule.setSeverity(request.getSeverity());
            if (request.getEnabled() != null) rule.setEnabled(request.getEnabled());
            rule.setUpdateTime(java.time.LocalDateTime.now());
            ruleMapper.updateById(rule);
        }
    }

    /**
     * 切换规则启用状态（Controller调用）
     */
    public void toggleRule(Long id, Boolean enabled, Long operator) {
        if (enabled == null) enabled = true;
        toggleRule(id, enabled);
    }

    /**
     * 重新加载规则
     */
    public void reloadRules() {
        try {
            List<ReviewRule> rules = ruleMapper.selectAllEnabled();
            ruleEngine.loadRules(rules);
            
            log.info("规则重新加载完成: ruleCount={}", rules.size());
            
        } catch (Exception e) {
            log.error("规则重新加载失败: error={}", e.getMessage(), e);
        }
    }

    /**
     * 获取规则详情
     * 
     * @param id 规则ID
     * @return 规则实体
     */
    public ReviewRule getRuleDetail(Long id) {
        return ruleMapper.selectById(id);
    }

    /**
     * 获取规则详情（VO版本，接口实现）
     */
    public com.codereview.vo.RuleVO getRuleDetailAsVO(Long id) {
        ReviewRule rule = ruleMapper.selectById(id);
        if (rule == null) return null;
        return com.codereview.vo.RuleVO.builder()
                .ruleId(rule.getId())
                .name(rule.getName())
                .description(rule.getDescription())
                .category(rule.getCategory() != null ? rule.getCategory().getName() : null)
                .severity(rule.getSeverity())
                .enabled(rule.getEnabled())
                .createdBy(rule.getCreatorId())
                .createdAt(rule.getCreateTime())
                .updatedAt(rule.getUpdateTime())
                .build();
    }

    /**
     * 根据规则标识获取规则
     * 
     * @param ruleId 规则标识
     * @return 规则实体
     */
    public ReviewRule getRuleByRuleId(String ruleId) {
        return ruleMapper.selectByRuleId(ruleId);
    }
}
