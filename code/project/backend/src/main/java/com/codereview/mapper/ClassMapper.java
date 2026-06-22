package com.codereview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codereview.entity.ClassEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 班级Mapper接口
 */
@Mapper
public interface ClassMapper extends BaseMapper<ClassEntity> {
}
