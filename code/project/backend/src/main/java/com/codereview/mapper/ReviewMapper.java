package com.codereview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codereview.entity.Review;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审查记录Mapper接口
 */
@Mapper
public interface ReviewMapper extends BaseMapper<Review> {
}
