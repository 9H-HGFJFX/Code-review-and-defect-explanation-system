package com.codereview.service;

import com.codereview.common.result.PageResult;
import com.codereview.dto.CreateTaskRequest;
import com.codereview.dto.PageRequest;
import com.codereview.vo.TaskDetailVO;
import com.codereview.vo.TaskVO;

/**
 * 审查任务服务接口
 */
public interface ReviewTaskServiceInterface {

    /**
     * 创建审查任务
     *
     * @param request 创建请求
     * @param creatorId 创建人ID
     * @return 任务ID
     */
    Long createTask(CreateTaskRequest request, Long creatorId);

    /**
     * 分页查询任务列表
     *
     * @param pageRequest 分页请求
     * @param classId 班级ID（可选）
     * @param status 状态（可选）
     * @param keyword 关键词（可选）
     * @return 分页结果
     */
    PageResult<TaskVO> getTaskList(PageRequest pageRequest, Long classId, String status, String keyword);

    /**
     * 获取任务详情
     *
     * @param taskId 任务ID
     * @return 任务详情VO
     */
    TaskDetailVO getTaskDetail(Long taskId);

    /**
     * 更新任务状态
     *
     * @param taskId 任务ID
     * @param status 新状态
     * @param operator 操作人ID
     */
    void updateTaskStatus(Long taskId, String status, Long operator);

    /**
     * 删除任务
     *
     * @param taskId 任务ID
     */
    void deleteTask(Long taskId);
}
