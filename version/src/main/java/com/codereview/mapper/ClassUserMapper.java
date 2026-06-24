package com.codereview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codereview.entity.ClassUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 班级用户Mapper
 */
@Mapper
public interface ClassUserMapper extends BaseMapper<ClassUser> {

    /**
     * 统计班级的学生数量
     */
    @Select("SELECT COUNT(*) FROM class_user WHERE class_id = #{classId}")
    int countByClassId(@Param("classId") Long classId);
}
