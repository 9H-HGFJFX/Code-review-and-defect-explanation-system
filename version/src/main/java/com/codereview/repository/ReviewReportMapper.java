package com.codereview.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codereview.entity.ReviewReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 审查报告Mapper接口
 * 
 * @author code-review-team
 */
@Mapper
public interface ReviewReportMapper extends BaseMapper<ReviewReport> {

    /**
     * 根据任务ID查询报告列表
     * 
     * @param taskId 任务ID
     * @return 报告列表
     */
    @Select("SELECT * FROM review_report WHERE task_id = #{taskId} AND deleted = 0 ORDER BY create_time DESC")
    List<ReviewReport> selectByTaskId(@Param("taskId") Long taskId);

    /**
     * 根据创建人查询报告列表
     * 
     * @param creatorId 创建人ID
     * @return 报告列表
     */
    @Select("SELECT * FROM review_report WHERE creator_id = #{creatorId} AND deleted = 0 ORDER BY create_time DESC")
    List<ReviewReport> selectByCreatorId(@Param("creatorId") Long creatorId);
}
