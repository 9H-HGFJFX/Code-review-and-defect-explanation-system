package com.codereview.service;

import com.codereview.dto.CreateClassRequest;
import com.codereview.common.result.PageResult;
import com.codereview.vo.ClassVO;

import java.util.List;

/**
 * 班级服务接口
 */
public interface ClassService {

    /**
     * 创建班级
     *
     * @param request   创建请求
     * @param creatorId 创建者ID
     * @return 班级ID
     */
    Long createClass(CreateClassRequest request, Long creatorId);

    /**
     * 获取班级列表
     *
     * @param classId 班级ID（可选，用于过滤）
     * @return 班级列表
     */
    List<ClassVO> getClassList(Long classId);

    /**
     * 获取班级详情
     *
     * @param classId 班级ID
     * @return 班级详情
     */
    ClassVO getClassDetail(Long classId);

    /**
     * 更新班级
     *
     * @param classId  班级ID
     * @param request  更新请求
     * @param operator 操作人ID
     */
    void updateClass(Long classId, CreateClassRequest request, Long operator);

    /**
     * 删除班级
     *
     * @param classId  班级ID
     * @param operator 操作人ID
     */
    void deleteClass(Long classId, Long operator);

    /**
     * 添加学生到班级
     *
     * @param classId  班级ID
     * @param studentId 学生ID
     * @param operator 操作人ID
     */
    void addStudent(Long classId, Long studentId, Long operator);

    /**
     * 获取班级学生列表
     *
     * @param classId 班级ID
     * @return 学生列表
     */
    List<ClassVO> getClassStudents(Long classId);
}
