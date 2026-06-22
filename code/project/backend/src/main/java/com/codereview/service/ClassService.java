package com.codereview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codereview.common.PageRequest;
import com.codereview.common.PageResponse;
import com.codereview.dto.ClassMemberRequest;
import com.codereview.dto.ClassRequest;
import com.codereview.entity.ClassEntity;
import com.codereview.entity.ClassUser;
import com.codereview.entity.User;
import com.codereview.exception.BusinessException;
import com.codereview.mapper.ClassMapper;
import com.codereview.mapper.ClassUserMapper;
import com.codereview.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 班级管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassService extends ServiceImpl<ClassMapper, ClassEntity> {
    
    private final ClassUserMapper classUserMapper;
    private final UserMapper userMapper;
    
    /**
     * 创建班级
     */
    @Transactional
    public ClassEntity createClass(Long teacherId, ClassRequest request) {
        ClassEntity classEntity = new ClassEntity();
        classEntity.setClassName(request.getClassName());
        classEntity.setDescription(request.getDescription());
        classEntity.setTeacherId(teacherId);
        
        save(classEntity);
        
        log.info("创建班级: {} by teacher: {}", request.getClassName(), teacherId);
        return classEntity;
    }
    
    /**
     * 更新班级信息
     */
    @Transactional
    public ClassEntity updateClass(Long classId, Long teacherId, ClassRequest request) {
        ClassEntity classEntity = getById(classId);
        if (classEntity == null) {
            throw new BusinessException(40001, "班级不存在");
        }
        
        // 检查权限（只有管理员或班级教师可以更新）
        if (!classEntity.getTeacherId().equals(teacherId)) {
            throw new BusinessException(10005, "无权限操作此班级");
        }
        
        classEntity.setClassName(request.getClassName());
        classEntity.setDescription(request.getDescription());
        
        updateById(classEntity);
        
        log.info("更新班级: {}", classId);
        return classEntity;
    }
    
    /**
     * 删除班级
     */
    @Transactional
    public void deleteClass(Long classId, Long teacherId) {
        ClassEntity classEntity = getById(classId);
        if (classEntity == null) {
            throw new BusinessException(40001, "班级不存在");
        }
        
        // 检查权限
        if (!classEntity.getTeacherId().equals(teacherId)) {
            throw new BusinessException(10005, "无权限操作此班级");
        }
        
        // 删除班级成员关系
        classUserMapper.delete(new LambdaQueryWrapper<ClassUser>().eq(ClassUser::getClassId, classId));
        
        // 删除班级
        removeById(classId);
        
        log.info("删除班级: {}", classId);
    }
    
    /**
     * 分页查询班级列表
     */
    public PageResponse<ClassEntity> getClassList(PageRequest pageRequest, Long teacherId) {
        Page<ClassEntity> page = new Page<>(pageRequest.getCurrent(), pageRequest.getSize());
        
        LambdaQueryWrapper<ClassEntity> wrapper = new LambdaQueryWrapper<>();
        if (teacherId != null) {
            wrapper.eq(ClassEntity::getTeacherId, teacherId);
        }
        wrapper.orderByDesc(ClassEntity::getCreateTime);
        
        Page<ClassEntity> resultPage = page(page, wrapper);
        
        // 查询每个班级的学生数量
        for (ClassEntity classEntity : resultPage.getRecords()) {
            long studentCount = classUserMapper.selectCount(
                new LambdaQueryWrapper<ClassUser>().eq(ClassUser::getClassId, classEntity.getId())
            );
            // 可以将学生数量存储在某个字段中，或创建DTO返回
        }
        
        return PageResponse.ok(
            resultPage.getRecords(),
            resultPage.getTotal(),
            resultPage.getCurrent(),
            resultPage.getSize()
        );
    }
    
    /**
     * 获取班级详情
     */
    public ClassEntity getClassDetail(Long classId) {
        ClassEntity classEntity = getById(classId);
        if (classEntity == null) {
            throw new BusinessException(40001, "班级不存在");
        }
        return classEntity;
    }
    
    /**
     * 添加班级成员
     */
    @Transactional
    public void addClassMembers(Long classId, Long teacherId, ClassMemberRequest request) {
        ClassEntity classEntity = getById(classId);
        if (classEntity == null) {
            throw new BusinessException(40001, "班级不存在");
        }
        
        // 检查权限
        if (!classEntity.getTeacherId().equals(teacherId)) {
            throw new BusinessException(10005, "无权限操作此班级");
        }
        
        List<ClassUser> members = new ArrayList<>();
        for (Long userId : request.getUserIds()) {
            // 检查用户是否为学生
            User user = userMapper.selectById(userId);
            if (user == null || !User.ROLE_STUDENT.equals(user.getRole())) {
                log.warn("用户{}不是学生角色，无法加入班级", userId);
                continue;
            }
            
            // 检查是否已在班级中
            if (classUserMapper.selectCount(
                    new LambdaQueryWrapper<ClassUser>()
                        .eq(ClassUser::getClassId, classId)
                        .eq(ClassUser::getUserId, userId)
                ).intValue() > 0) {
                log.info("用户{}已在班级{}中", userId, classId);
                continue;
            }
            
            ClassUser member = new ClassUser();
            member.setClassId(classId);
            member.setUserId(userId);
            members.add(member);
        }
        
        if (!members.isEmpty()) {
            saveBatch(members);
            log.info("添加{}个学生到班级{}", members.size(), classId);
        }
    }
    
    /**
     * 移除班级成员
     */
    @Transactional
    public void removeClassMembers(Long classId, Long teacherId, List<Long> userIds) {
        ClassEntity classEntity = getById(classId);
        if (classEntity == null) {
            throw new BusinessException(40001, "班级不存在");
        }
        
        // 检查权限
        if (!classEntity.getTeacherId().equals(teacherId)) {
            throw new BusinessException(10005, "无权限操作此班级");
        }
        
        classUserMapper.delete(
            new LambdaQueryWrapper<ClassUser>()
                .eq(ClassUser::getClassId, classId)
                .in(ClassUser::getUserId, userIds)
        );
        
        log.info("从班级{}移除{}个学生", classId, userIds.size());
    }
    
    /**
     * 获取班级成员列表
     */
    public List<User> getClassMembers(Long classId) {
        List<ClassUser> classUsers = classUserMapper.selectList(
            new LambdaQueryWrapper<ClassUser>().eq(ClassUser::getClassId, classId)
        );
        
        List<Long> userIds = classUsers.stream()
                .map(ClassUser::getUserId)
                .collect(Collectors.toList());
        
        if (userIds.isEmpty()) {
            return List.of();
        }
        
        return userMapper.selectBatchIds(userIds);
    }
    
    /**
     * 获取用户加入的班级列表
     */
    public List<ClassEntity> getUserClasses(Long userId) {
        List<ClassUser> classUsers = classUserMapper.selectList(
            new LambdaQueryWrapper<ClassUser>().eq(ClassUser::getUserId, userId)
        );
        
        if (classUsers.isEmpty()) {
            return List.of();
        }
        
        List<Long> classIds = classUsers.stream()
                .map(ClassUser::getClassId)
                .collect(Collectors.toList());
        
        return listByIds(classIds);
    }
    
    /**
     * 检查用户是否在班级中
     */
    public boolean isUserInClass(Long userId, Long classId) {
        return classUserMapper.selectCount(
            new LambdaQueryWrapper<ClassUser>()
                .eq(ClassUser::getClassId, classId)
                .eq(ClassUser::getUserId, userId)
        ).intValue() > 0;
    }
}
