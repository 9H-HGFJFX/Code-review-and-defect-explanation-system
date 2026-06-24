package com.codereview.service;

import com.codereview.common.enums.IssueSeverity;
import com.codereview.common.enums.IssueStatus;
import com.codereview.dto.PageRequest;
import com.codereview.common.result.PageResult;
import com.codereview.entity.CodeIssue;
import com.codereview.vo.IssueVO;

import java.util.List;

/**
 * 缺陷服务接口
 */
public interface IssueServiceInterface {

    /**
     * 分页获取任务下的缺陷列表
     */
    PageResult<IssueVO> getIssueListByTask(Long taskId, PageRequest pageRequest,
                                           String severity, String status, Long assigneeId);

    /**
     * 分配缺陷
     */
    void assignIssue(Long issueId, Long assigneeId, Long operator);

    /**
     * 批量分配缺陷
     */
    int batchAssign(List<Long> issueIds, Long assigneeId, Long assigneeName);

    /**
     * 更新缺陷状态
     */
    void updateIssueStatus(Long issueId, String status, Long operator);

    /**
     * 批量更新缺陷状态
     */
    int batchUpdateStatus(List<Long> issueIds, IssueStatus status);

    /**
     * 添加缺陷备注
     */
    boolean addRemark(Long issueId, String remark);

    /**
     * 获取缺陷详情
     */
    CodeIssue getDetail(Long issueId);

    /**
     * 统计任务中各严重程度的缺陷数量
     */
    IssueService.IssueCountStats getStatsByTask(Long taskId);

    /**
     * 缺陷数量统计
     */
    class IssueCountStats {
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
}
