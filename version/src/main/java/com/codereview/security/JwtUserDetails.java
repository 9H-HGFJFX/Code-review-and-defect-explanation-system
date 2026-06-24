package com.codereview.security;

import com.codereview.common.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * JWT令牌中的用户信息
 * 存储在SecurityContext中供后续使用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtUserDetails implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 角色
     */
    private UserRole role;

    /**
     * 班级ID（用于数据隔离）
     */
    private Long classId;

    /**
     * 判断是否为管理员
     */
    public boolean isAdmin() {
        return role != null && role.isAdmin();
    }

    /**
     * 判断是否为教师
     */
    public boolean isTeacher() {
        return role != null && role.isTeacher();
    }

    /**
     * 判断是否为学生
     */
    public boolean isStudent() {
        return role != null && role.isStudent();
    }

    /**
     * 判断是否可以创建任务
     */
    public boolean canCreateTask() {
        return isTeacher();
    }

    /**
     * 判断是否可以删除任务
     */
    public boolean canDeleteTask() {
        return isAdmin();
    }

    /**
     * 判断是否可以管理规则
     */
    public boolean canManageRules() {
        return isAdmin();
    }

    /**
     * 判断是否可以管理班级
     */
    public boolean canManageClass() {
        return isAdmin();
    }

    /**
     * 判断是否可以分配缺陷
     */
    public boolean canAssignIssue() {
        return isTeacher();
    }

    /**
     * 判断是否可以更新任务状态
     */
    public boolean canUpdateTaskStatus(Long taskCreatorId) {
        return isTeacher() && taskCreatorId.equals(userId);
    }
}
