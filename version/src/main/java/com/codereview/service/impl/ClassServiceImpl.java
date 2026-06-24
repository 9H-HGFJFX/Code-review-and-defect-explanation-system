package com.codereview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codereview.common.enums.ErrorCode;
import com.codereview.common.enums.UserRole;
import com.codereview.common.exception.BusinessException;
import com.codereview.common.exception.ResourceNotFoundException;
import com.codereview.config.DataScopeFilter;
import com.codereview.dto.CreateClassRequest;
import com.codereview.entity.ClassEntity;
import com.codereview.entity.ClassUser;
import com.codereview.entity.User;
import com.codereview.mapper.ClassMapper;
import com.codereview.mapper.ClassUserMapper;
import com.codereview.mapper.UserMapper;
import com.codereview.service.ClassService;
import com.codereview.vo.ClassVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 班级服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassServiceImpl implements ClassService {

    private final ClassMapper classMapper;
    private final ClassUserMapper classUserMapper;
    private final UserMapper userMapper;
    private final DataScopeFilter dataScopeFilter;

    @Override
    @Transactional
    public Long createClass(CreateClassRequest request, Long creatorId) {
        // 验证班主任存在
        User teacher = userMapper.selectById(request.getTeacherId());
        if (teacher == null) {
            throw new BusinessException(ErrorCode.CLASS_CREATE_FAILED, "Teacher not found");
        }

        ClassEntity classEntity = ClassEntity.builder()
                .name(request.getName())
                .teacherId(request.getTeacherId())
                .description(request.getDescription())
                .build();

        classMapper.insert(classEntity);
        log.info("Class created: classId={}, name={}, teacher={}", classEntity.getId(), classEntity.getName(), request.getTeacherId());

        return classEntity.getId();
    }

    @Override
    public List<ClassVO> getClassList(Long classId) {
        LambdaQueryWrapper<ClassEntity> wrapper = new LambdaQueryWrapper<>();

        // SUPER_ADMIN可以看所有班级
        // TEACHER只能看自己班级的
        if (!dataScopeFilter.isSuperAdmin()) {
            Long filterClassId = dataScopeFilter.getClassIdFilter();
            if (filterClassId != null) {
                wrapper.eq(ClassEntity::getId, filterClassId);
            } else if (classId != null) {
                wrapper.eq(ClassEntity::getId, classId);
            } else {
                // 没有classId过滤且不是admin，返回空列表
                return new ArrayList<>();
            }
        } else if (classId != null) {
            wrapper.eq(ClassEntity::getId, classId);
        }

        List<ClassEntity> classes = classMapper.selectList(wrapper);

        return classes.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public ClassVO getClassDetail(Long classId) {
        ClassEntity classEntity = classMapper.selectById(classId);
        if (classEntity == null) {
            throw new ResourceNotFoundException("Class", classId);
        }

        // 数据隔离检查
        Long filterClassId = dataScopeFilter.getClassIdFilter();
        if (filterClassId != null && !filterClassId.equals(classId)) {
            throw new BusinessException(ErrorCode.TASK_NOT_IN_CLASS);
        }

        return convertToVO(classEntity);
    }

    @Override
    @Transactional
    public void updateClass(Long classId, CreateClassRequest request, Long operator) {
        ClassEntity classEntity = classMapper.selectById(classId);
        if (classEntity == null) {
            throw new ResourceNotFoundException("Class", classId);
        }

        if (StringUtils.hasText(request.getName())) {
            classEntity.setName(request.getName());
        }
        if (request.getTeacherId() != null) {
            classEntity.setTeacherId(request.getTeacherId());
        }
        if (request.getDescription() != null) {
            classEntity.setDescription(request.getDescription());
        }

        classMapper.updateById(classEntity);
        log.info("Class updated: classId={}, operator={}", classId, operator);
    }

    @Override
    @Transactional
    public void deleteClass(Long classId, Long operator) {
        classMapper.deleteById(classId);
        log.info("Class deleted: classId={}, operator={}", classId, operator);
    }

    @Override
    @Transactional
    public void addStudent(Long classId, Long studentId, Long operator) {
        // 验证班级存在
        ClassEntity classEntity = classMapper.selectById(classId);
        if (classEntity == null) {
            throw new ResourceNotFoundException("Class", classId);
        }

        // 验证学生存在
        User student = userMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException(ErrorCode.STUDENT_NOT_FOUND, "Student not found");
        }

        // 检查是否已添加
        LambdaQueryWrapper<ClassUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassUser::getClassId, classId)
                .eq(ClassUser::getUserId, studentId);
        long count = classUserMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.STUDENT_NOT_FOUND, "Student already in class");
        }

        // 添加关联
        ClassUser classUser = ClassUser.builder()
                .classId(classId)
                .userId(studentId)
                .roleInClass(UserRole.STUDENT.getCode())
                .build();

        classUserMapper.insert(classUser);

        // 更新学生的班级ID
        student.setClassId(classId);
        userMapper.updateById(student);

        log.info("Student added to class: classId={}, studentId={}, operator={}", classId, studentId, operator);
    }

    @Override
    public List<ClassVO> getClassStudents(Long classId) {
        // 获取班级信息
        ClassEntity classEntity = classMapper.selectById(classId);
        if (classEntity == null) {
            throw new ResourceNotFoundException("Class", classId);
        }

        // 获取班级学生
        LambdaQueryWrapper<ClassUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassUser::getClassId, classId);

        List<ClassUser> classUsers = classUserMapper.selectList(wrapper);
        List<Long> userIds = classUsers.stream()
                .map(ClassUser::getUserId)
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<User> students = userMapper.selectBatchIds(userIds);

        return students.stream()
                .map(user -> ClassVO.builder()
                        .classId(classId)
                        .studentCount(students.size())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 转换为班级VO
     */
    private ClassVO convertToVO(ClassEntity classEntity) {
        User teacher = userMapper.selectById(classEntity.getTeacherId());
        int studentCount = classUserMapper.countByClassId(classEntity.getId());

        return ClassVO.builder()
                .classId(classEntity.getId())
                .name(classEntity.getName())
                .teacherId(classEntity.getTeacherId())
                .teacherName(teacher != null ? teacher.getUsername() : null)
                .description(classEntity.getDescription())
                .studentCount(studentCount)
                .createdAt(classEntity.getCreatedAt())
                .build();
    }
}
