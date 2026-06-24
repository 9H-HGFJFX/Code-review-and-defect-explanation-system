package com.codereview.service;

import com.codereview.dto.CreateRuleRequest;
import com.codereview.dto.PageRequest;
import com.codereview.dto.UpdateRuleRequest;
import com.codereview.common.result.PageResult;
import com.codereview.entity.ReviewRule;
import com.codereview.vo.RuleVO;

import java.util.List;

/**
 * 规则服务接口
 */
public interface RuleServiceInterface {

    /**
     * 分页获取规则列表
     */
    PageResult<RuleVO> getRuleList(PageRequest pageRequest, Boolean enabled, String category);

    /**
     * 创建规则
     */
    Long createRule(CreateRuleRequest request, Long creatorId);

    /**
     * 更新规则
     */
    void updateRule(Long ruleId, UpdateRuleRequest request, Long operator);

    /**
     * 切换规则启用状态
     */
    void toggleRule(Long ruleId, Boolean enabled, Long operator);

    /**
     * 获取规则详情
     */
    ReviewRule getRuleDetail(Long id);

    /**
     * 根据规则标识获取规则
     */
    ReviewRule getRuleByRuleId(String ruleId);

    /**
     * 获取所有启用的规则
     */
    List<ReviewRule> listEnabledRules();

    /**
     * 启用/禁用规则
     */
    boolean toggleRule(Long id, boolean enabled);

    /**
     * 删除规则
     */
    boolean deleteRule(Long id);

    /**
     * 重新加载规则
     */
    void reloadRules();
}
