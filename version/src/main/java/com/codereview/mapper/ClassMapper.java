package com.codereview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codereview.entity.ClassEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 班级Mapper
 */
@Mapper
public interface ClassMapper extends BaseMapper<ClassEntity> {

    /**
     * 根据ID查询班级
     */
    @Select("SELECT * FROM class WHERE id = #{id}")
    ClassEntity selectById(@Param("id") Long id);
}
