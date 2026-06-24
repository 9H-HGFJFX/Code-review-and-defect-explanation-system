package com.codereview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codereview.entity.ReviewTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 审查任务Mapper
 */
@Mapper
public interface ReviewTaskMapper extends BaseMapper<ReviewTask> {

    /**
     * 根据ID查询任务
     */
    @Select("SELECT * FROM review_task WHERE id = #{id}")
    ReviewTask selectById(@Param("id") Long id);
}
