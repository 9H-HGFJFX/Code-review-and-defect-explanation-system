package com.codereview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codereview.common.enums.IssueSeverity;
import com.codereview.common.enums.IssueStatus;
import com.codereview.entity.CodeIssue;
import com.codereview.repository.CodeIssueMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 缺陷服务实现
 * 提供缺陷查询、分配、状态更新功能
 * 
 * @author code-review-team
 */
@Service
@Slf4j
public class IssueService implements IssueServiceInterface {

    @Autowired
    private CodeIssueMapper issueMapper;

    /**
     * 根据任务ID查询缺陷列表
     * 
     * @param taskId 任务ID
     * @return 缺陷列表
     */
    public List<CodeIssue> listByTask(Long taskId) {
        return issueMapper.selectByTaskId(taskId);
    }

    /**
     * 根据任务ID和严重程度查询缺陷
     * 
     * @param taskId 任务ID
     * @param severity 严重程度
     * @return 缺陷列表
     */
    public List<CodeIssue> listByTaskAndSeverity(Long taskId, IssueSeverity severity) {
        return issueMapper.selectByTaskIdAndSeverity(taskId, severity);
    }

    /**
     * 查询待分配的缺陷
     * 
     * @param taskId 任务ID
     * @return 缺陷列表
     */
    public List<CodeIssue> listUnassigned(Long taskId) {
        return issueMapper.selectUnassignedByTaskId(taskId);
    }

    /**
     * 分配缺陷
     * 
     * @param issueId 缺陷ID
     * @param assigneeId 责任人ID
     * @param assigneeName 责任人姓名
     * @return 是否分配成功
     */
    @Transactional
    public boolean assignIssue(Long issueId, Long assigneeId, String assigneeName) {
        try {
            CodeIssue issue = issueMapper.selectById(issueId);
            if (issue != null) {
                issue.setAssigneeId(assigneeId);
                issue.setAssigneeName(assigneeName);
                issue.setUpdateTime(LocalDateTime.now());
                issueMapper.updateById(issue);
                
                log.info("缺陷已分配: issueId={}, assigneeId={}, assigneeName={}", 
                    issueId, assigneeId, assigneeName);
                return true;
            }
        } catch (Exception e) {
            log.error("分配缺陷失败: issueId={}, error={}", issueId, e.getMessage(), e);
        }
        return false;
    }

    /**
     * 批量分配缺陷
     * 
     * @param issueIds 缺陷ID列表
     * @param assigneeId 责任人ID
     * @param assigneeName 责任人姓名
     * @return 分配成功的数量
     */
    @Transactional
    public int batchAssign(List<Long> issueIds, Long assigneeId, String assigneeName) {
        int count = 0;
        for (Long issueId : issueIds) {
            if (assignIssue(issueId, assigneeId, assigneeName)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 更新缺陷状态
     * 
     * @param issueId 缺陷ID
     * @param status 新状态
     * @return 是否更新成功
     */
    @Transactional
    public boolean updateStatus(Long issueId, IssueStatus status) {
        try {
            CodeIssue issue = issueMapper.selectById(issueId);
            if (issue != null) {
                issue.setStatus(status);
                issue.setUpdateTime(LocalDateTime.now());
                issueMapper.updateById(issue);
                
                log.info("缺陷状态更新: issueId={}, status={}", issueId, status);
                return true;
            }
        } catch (Exception e) {
            log.error("更新缺陷状态失败: issueId={}, error={}", issueId, e.getMessage(), e);
        }
        return false;
    }

    /**
     * 批量更新缺陷状态
     * 
     * @param issueIds 缺陷ID列表
     * @param status 新状态
     * @return 更新成功的数量
     */
    @Transactional
    public int batchUpdateStatus(List<Long> issueIds, IssueStatus status) {
        return issueMapper.batchUpdateStatus(issueIds, status);
    }

    /**
     * 添加缺陷备注
     * 
     * @param issueId 缺陷ID
     * @param remark 备注内容
     * @return 是否添加成功
     */
    @Transactional
    public boolean addRemark(Long issueId, String remark) {
        try {
            CodeIssue issue = issueMapper.selectById(issueId);
            if (issue != null) {
                String existingRemark = issue.getRemark();
                if (existingRemark != null && !existingRemark.isEmpty()) {
                    issue.setRemark(existingRemark + "\n" + remark);
                } else {
                    issue.setRemark(remark);
                }
                issue.setUpdateTime(LocalDateTime.now());
                issueMapper.updateById(issue);
                return true;
            }
        } catch (Exception e) {
            log.error("添加备注失败: issueId={}, error={}", issueId, e.getMessage(), e);
        }
        return false;
    }

    /**
     * 获取缺陷详情
     * 
     * @param issueId 缺陷ID
     * @return 缺陷实体
     */
    public CodeIssue getDetail(Long issueId) {
        return issueMapper.selectById(issueId);
    }

    /**
     * 统计任务中各严重程度的缺陷数量
     * 
     * @param taskId 任务ID
     * @return 统计结果
     */
    public IssueCountStats getStatsByTask(Long taskId) {
        IssueCountStats stats = new IssueCountStats();
        
        List<CodeIssue> issues = issueMapper.selectByTaskId(taskId);
        
        stats.setTotal(issues.size());
        
        for (CodeIssue issue : issues) {
            if (issue.getSeverity() != null) {
                switch (issue.getSeverity()) {
                    case CRITICAL:
                        stats.setCritical(stats.getCritical() + 1);
                        break;
                    case HIGH:
                        stats.setHigh(stats.getHigh() + 1);
                        break;
                    case MEDIUM:
                        stats.setMedium(stats.getMedium() + 1);
                        break;
                    case LOW:
                        stats.setLow(stats.getLow() + 1);
                        break;
                    case INFO:
                        stats.setInfo(stats.getInfo() + 1);
                        break;
                }
            }
        }
        
        return stats;
    }

    /**
     * 缺陷数量统计
     */
    public static class IssueCountStats {
        private int total;
        private int critical;
        private int high;
        private int medium;
        private int low;
        private int info;

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getCritical() { return critical; }
        public void setCritical(int critical) { this.critical = critical; }
        public int getHigh() { return high; }
        public void setHigh(int high) { this.high = high; }
        public int getMedium() { return medium; }
        public void setMedium(int medium) { this.medium = medium; }
        public int getLow() { return low; }
        public void setLow(int low) { this.low = low; }
        public int getInfo() { return info; }
        public void setInfo(int info) { this.info = info; }
    }

    // --- IssueServiceInterface required methods ---

    @Override
    public com.codereview.common.result.PageResult<com.codereview.vo.IssueVO> getIssueListByTask(
            Long taskId, com.codereview.dto.PageRequest pageRequest,
            String severity, String status, Long assigneeId) {
        // 实现由 IssueServiceImpl 提供，此处为空桩以满足接口
        return new com.codereview.common.result.PageResult<>(
                new java.util.ArrayList<>(),
                com.codereview.common.result.PageResult.Pagination.of(pageRequest.getPage(), pageRequest.getPageSize(), 0));
    }

    @Override
    public void assignIssue(Long issueId, Long assigneeId, Long operator) {
        // operator is userId, assigneeName from ID would need user lookup
        assignIssue(issueId, assigneeId, "user-" + assigneeId);
    }

    @Override
    public void updateIssueStatus(Long issueId, String status, Long operator) {
        IssueStatus newStatus = IssueStatus.fromName(status);
        updateStatus(issueId, newStatus);
    }

    @Override
    public int batchAssign(java.util.List<Long> issueIds, Long assigneeId, Long assigneeName) {
        return batchAssign(issueIds, assigneeId, "user-" + assigneeId);
    }
}
