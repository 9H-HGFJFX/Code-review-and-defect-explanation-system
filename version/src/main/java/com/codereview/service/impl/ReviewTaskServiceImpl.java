package com.codereview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codereview.common.enums.ErrorCode;
import com.codereview.common.enums.TaskStatus;
import com.codereview.common.exception.BusinessException;
import com.codereview.common.exception.ResourceNotFoundException;
import com.codereview.config.DataScopeFilter;
import com.codereview.dto.CreateTaskRequest;
import com.codereview.dto.PageRequest;
import com.codereview.common.result.PageResult;
import com.codereview.entity.ClassEntity;
import com.codereview.entity.ReviewTask;
import com.codereview.entity.User;
import com.codereview.mapper.ClassMapper;
import com.codereview.mapper.IssueMapper;
import com.codereview.mapper.ReviewTaskMapper;
import com.codereview.mapper.UserMapper;
import com.codereview.service.ReviewTaskServiceInterface;
import com.codereview.vo.TaskDetailVO;
import com.codereview.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 审查任务服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewTaskServiceImpl implements ReviewTaskServiceInterface {

    private final ReviewTaskMapper reviewTaskMapper;
    private final IssueMapper issueMapper;
    private final UserMapper userMapper;
    private final ClassMapper classMapper;
    private final DataScopeFilter dataScopeFilter;

    @Override
    @Transactional
    public Long createTask(CreateTaskRequest request, Long creatorId) {
        // 1. 验证创建者权限
        User creator = userMapper.selectById(creatorId);
        if (creator == null) {
            throw new BusinessException(ErrorCode.TASK_CREATE_FAILED, "Creator not found");
        }

        // 2. 创建任务
        ReviewTask task = ReviewTask.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TaskStatus.PENDING.getCode())
                .submitterId(creatorId)
                .reviewerId(creatorId)
                .classId(request.getClassId())
                .sourcePath(request.getRepoUrl())
                .ruleSet(request.getRuleIds() != null ?
                        String.join(",", request.getRuleIds().toString()) : "default")
                .deadline(request.getDeadline() != null ?
                        LocalDateTime.parse(request.getDeadline()) : null)
                .build();

        reviewTaskMapper.insert(task);
        log.info("Task created: taskId={}, title={}, creator={}", task.getId(), task.getTitle(), creatorId);

        return task.getId();
    }

    @Override
    public PageResult<TaskVO> getTaskList(PageRequest pageRequest, Long classId, String status, String keyword) {
        pageRequest.validate();

        // 构建查询条件
        LambdaQueryWrapper<ReviewTask> wrapper = new LambdaQueryWrapper<>();

        // 数据隔离：STUDENT和TEACHER只能看自己班级的任务
        Long filterClassId = dataScopeFilter.getClassIdFilter();
        if (filterClassId != null) {
            wrapper.eq(ReviewTask::getClassId, filterClassId);
        } else if (classId != null) {
            wrapper.eq(ReviewTask::getClassId, classId);
        }

        // 状态过滤
        if (StringUtils.hasText(status)) {
            TaskStatus taskStatus = TaskStatus.fromName(status);
            wrapper.eq(ReviewTask::getStatus, taskStatus.getCode());
        }

        // 关键词搜索
        if (StringUtils.hasText(keyword)) {
            wrapper.like(ReviewTask::getTitle, keyword);
        }

        // 排序
        String sortField = StringUtils.hasText(pageRequest.getSortBy()) ?
                pageRequest.getSortBy() : "createdAt";
        if ("desc".equalsIgnoreCase(pageRequest.getSortOrder())) {
            wrapper.orderByDesc(ReviewTask::getCreatedAt);
        } else {
            wrapper.orderByAsc(ReviewTask::getCreatedAt);
        }

        // 分页查询
        Page<ReviewTask> page = new Page<>(pageRequest.getPage(), pageRequest.getPageSize());
        IPage<ReviewTask> resultPage = reviewTaskMapper.selectPage(page, wrapper);

        // 转换为VO
        List<TaskVO> voList = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList,
                PageResult.Pagination.of(
                        (int) resultPage.getCurrent(),
                        (int) resultPage.getSize(),
                        resultPage.getTotal()
                ));
    }

    @Override
    public TaskDetailVO getTaskDetail(Long taskId) {
        ReviewTask task = reviewTaskMapper.selectById(taskId);
        if (task == null) {
            throw new ResourceNotFoundException("Task", taskId);
        }

        // 数据隔离检查
        Long filterClassId = dataScopeFilter.getClassIdFilter();
        if (filterClassId != null && !filterClassId.equals(task.getClassId())) {
            throw new BusinessException(ErrorCode.TASK_NOT_IN_CLASS);
        }

        // 获取创建者信息
        User creator = userMapper.selectById(task.getSubmitterId());

        // 获取班级信息
        ClassEntity classEntity = classMapper.selectById(task.getClassId());

        // 统计缺陷
        int issueCount = issueMapper.countByTaskId(taskId);
        int resolvedCount = issueMapper.countResolvedByTaskId(taskId);

        return TaskDetailVO.builder()
                .taskId(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(TaskStatus.fromCode(task.getStatus()).getName())
                .classId(task.getClassId())
                .className(classEntity != null ? classEntity.getName() : null)
                .createdBy(task.getSubmitterId())
                .creatorName(creator != null ? creator.getUsername() : null)
                .repoUrl(task.getSourcePath())
                .deadline(task.getDeadline())
                .issueCount(issueCount)
                .resolvedCount(resolvedCount)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public void updateTaskStatus(Long taskId, String status, Long operator) {
        ReviewTask task = reviewTaskMapper.selectById(taskId);
        if (task == null) {
            throw new ResourceNotFoundException("Task", taskId);
        }

        // 验证状态转换
        TaskStatus currentStatus = TaskStatus.fromCode(task.getStatus());
        TaskStatus newStatus = TaskStatus.fromName(status);

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new BusinessException(ErrorCode.TASK_STATUS_INVALID,
                    "Cannot transition from " + currentStatus.getName() + " to " + newStatus.getName());
        }

        // 更新状态
        task.setStatus(newStatus.getCode());
        reviewTaskMapper.updateById(task);

        log.info("Task status updated: taskId={}, from={}, to={}, operator={}",
                taskId, currentStatus.getName(), newStatus.getName(), operator);
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId) {
        reviewTaskMapper.deleteById(taskId);
        log.info("Task deleted: taskId={}", taskId);
    }

    /**
     * 转换为任务VO
     */
    private TaskVO convertToVO(ReviewTask task) {
        User creator = userMapper.selectById(task.getSubmitterId());
        ClassEntity classEntity = classMapper.selectById(task.getClassId());
        int issueCount = issueMapper.countByTaskId(task.getId());

        return TaskVO.builder()
                .taskId(task.getId())
                .title(task.getTitle())
                .status(TaskStatus.fromCode(task.getStatus()).getName())
                .classId(task.getClassId())
                .className(classEntity != null ? classEntity.getName() : null)
                .createdBy(task.getSubmitterId())
                .creatorName(creator != null ? creator.getUsername() : null)
                .issueCount(issueCount)
                .deadline(task.getDeadline())
                .createdAt(task.getCreatedAt())
                .build();
    }
}
