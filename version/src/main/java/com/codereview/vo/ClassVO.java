package com.codereview.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 班级VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassVO {

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 班级名称
     */
    private String name;

    /**
     * 班主任ID
     */
    private Long teacherId;

    /**
     * 班主任名称
     */
    private String teacherName;

    /**
     * 班级描述
     */
    private String description;

    /**
     * 学生数量
     */
    private Integer studentCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
