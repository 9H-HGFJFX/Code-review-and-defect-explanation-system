package com.codereview.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codereview.entity.ReviewTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 代码审查任务表 Mapper 接口
 */
@Mapper
public interface ReviewTaskMapper extends BaseMapper<ReviewTask> {
}