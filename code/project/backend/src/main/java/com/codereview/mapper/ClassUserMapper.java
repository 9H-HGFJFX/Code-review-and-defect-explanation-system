package com.codereview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codereview.entity.ClassUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 班级成员Mapper接口
 */
@Mapper
public interface ClassUserMapper extends BaseMapper<ClassUser> {
}
