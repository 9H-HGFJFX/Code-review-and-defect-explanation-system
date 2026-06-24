package com.codereview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codereview.common.enums.TaskStatus;
import com.codereview.common.result.PageResult;
import com.codereview.config.DataScopeFilter;
import com.codereview.dto.CreateTaskRequest;
import com.codereview.dto.PageRequest;
import com.codereview.entity.ClassEntity;
import com.codereview.entity.ReviewTask;
import com.codereview.entity.User;
import com.codereview.mapper.ClassMapper;
import com.codereview.mapper.IssueMapper;
import com.codereview.repository.ReviewTaskMapper;
import com.codereview.mapper.UserMapper;
import com.codereview.scheduler.ScanScheduler;
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
 * 审查任务服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewTaskService implements ReviewTaskServiceInterface {

    private final ReviewTaskMapper taskMapper;
    private final IssueMapper issueMapper;
    private final UserMapper userMapper;
    private final ClassMapper classMapper;
    private final DataScopeFilter dataScopeFilter;
    private final ScanScheduler scanScheduler;

    @Override
    @Transactional
    public Long createTask(CreateTaskRequest request, Long creatorId) {
        User creator = userMapper.selectById(creatorId);
        if (creator == null) {
            throw new com.codereview.common.exception.BusinessException(
                    com.codereview.common.enums.ErrorCode.TASK_CREATE_FAILED, "Creator not found");
        }

        ReviewTask task = ReviewTask.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TaskStatus.PENDING.getCode())
                .submitterId(creatorId)
                .reviewerId(creatorId)
                .classId(request.getClassId())
                .sourcePath(request.getRepoUrl())
                .deadline(request.getDeadline() != null ?
                        LocalDateTime.parse(request.getDeadline()) : null)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        taskMapper.insert(task);
        log.info("Task created: taskId={}, title={}, creator={}", task.getId(), task.getTitle(), creatorId);
        return task.getId();
    }

    @Override
    public PageResult<TaskVO> getTaskList(PageRequest pageRequest, Long classId, String status, String keyword) {
        pageRequest.validate();
        LambdaQueryWrapper<ReviewTask> wrapper = new LambdaQueryWrapper<>();

        Long filterClassId = dataScopeFilter.getClassIdFilter();
        if (filterClassId != null) {
            wrapper.eq(ReviewTask::getClassId, filterClassId);
        } else if (classId != null) {
            wrapper.eq(ReviewTask::getClassId, classId);
        }

        if (StringUtils.hasText(status)) {
            TaskStatus taskStatus = TaskStatus.fromName(status);
            wrapper.eq(ReviewTask::getStatus, taskStatus.getCode());
        }

        if (StringUtils.hasText(keyword)) {
            wrapper.like(ReviewTask::getTitle, keyword);
        }

        wrapper.orderByDesc(ReviewTask::getCreateTime);

        Page<ReviewTask> page = new Page<>(pageRequest.getPage(), pageRequest.getPageSize());
        IPage<ReviewTask> resultPage = taskMapper.selectPage(page, wrapper);

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
        ReviewTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new com.codereview.common.exception.ResourceNotFoundException("Task", taskId);
        }

        Long filterClassId = dataScopeFilter.getClassIdFilter();
        if (filterClassId != null && !filterClassId.equals(task.getClassId())) {
            throw new com.codereview.common.exception.BusinessException(
                    com.codereview.common.enums.ErrorCode.TASK_NOT_IN_CLASS);
        }

        User creator = userMapper.selectById(task.getSubmitterId());
        ClassEntity classEntity = classMapper.selectById(task.getClassId());
        int issueCount = issueMapper.countByTaskId(taskId);
        int resolvedCount = issueMapper.countResolvedByTaskId(taskId);

        return TaskDetailVO.builder()
                .taskId(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus() != null ? TaskStatus.fromCode(task.getStatus()).getName() : null)
                .classId(task.getClassId())
                .className(classEntity != null ? classEntity.getName() : null)
                .createdBy(task.getSubmitterId())
                .creatorName(creator != null ? creator.getUsername() : null)
                .repoUrl(task.getSourcePath())
                .deadline(task.getDeadline())
                .issueCount(issueCount)
                .resolvedCount(resolvedCount)
                .createdAt(task.getCreateTime())
                .updatedAt(task.getUpdateTime())
                .build();
    }

    @Override
    @Transactional
    public void updateTaskStatus(Long taskId, String status, Long operator) {
        ReviewTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new com.codereview.common.exception.ResourceNotFoundException("Task", taskId);
        }

        TaskStatus currentStatus = TaskStatus.fromCode(task.getStatus());
        TaskStatus newStatus = TaskStatus.fromName(status);

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new com.codereview.common.exception.BusinessException(
                    com.codereview.common.enums.ErrorCode.TASK_STATUS_INVALID,
                    "Cannot transition from " + currentStatus.getName() + " to " + newStatus.getName());
        }

        task.setStatus(newStatus.getCode());
        if (newStatus == TaskStatus.RUNNING) {
            task.setStartTime(LocalDateTime.now());
        } else if (newStatus == TaskStatus.COMPLETED || newStatus == TaskStatus.FAILED) {
            task.setEndTime(LocalDateTime.now());
        }
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);

        log.info("Task status updated: taskId={}, from={}, to={}, operator={}",
                taskId, currentStatus.getName(), newStatus.getName(), operator);
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId) {
        taskMapper.deleteById(taskId);
        log.info("Task deleted: taskId={}", taskId);
    }

    private TaskVO convertToVO(ReviewTask task) {
        User creator = userMapper.selectById(task.getSubmitterId());
        ClassEntity classEntity = classMapper.selectById(task.getClassId());
        int issueCount = issueMapper.countByTaskId(task.getId());

        return TaskVO.builder()
                .taskId(task.getId())
                .title(task.getTitle())
                .status(task.getStatus() != null ? TaskStatus.fromCode(task.getStatus()).getName() : null)
                .classId(task.getClassId())
                .className(classEntity != null ? classEntity.getName() : null)
                .createdBy(task.getSubmitterId())
                .creatorName(creator != null ? creator.getUsername() : null)
                .issueCount(issueCount)
                .deadline(task.getDeadline())
                .createdAt(task.getCreateTime())
                .build();
    }
}
