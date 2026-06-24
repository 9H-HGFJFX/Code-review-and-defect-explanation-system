package com.codereview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codereview.entity.Issue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 缺陷Mapper
 */
@Mapper
public interface IssueMapper extends BaseMapper<Issue> {

    /**
     * 根据ID查询缺陷
     */
    @Select("SELECT * FROM issue WHERE id = #{id}")
    Issue selectById(@Param("id") Long id);

    /**
     * 统计任务的缺陷数量
     */
    @Select("SELECT COUNT(*) FROM issue WHERE task_id = #{taskId}")
    int countByTaskId(@Param("taskId") Long taskId);

    /**
     * 统计任务已解决的缺陷数量
     */
    @Select("SELECT COUNT(*) FROM issue WHERE task_id = #{taskId} AND status = 3")
    int countResolvedByTaskId(@Param("taskId") Long taskId);
}
