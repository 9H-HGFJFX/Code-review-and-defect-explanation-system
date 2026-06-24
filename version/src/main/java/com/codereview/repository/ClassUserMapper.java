package com.codereview.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codereview.entity.ClassUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 班级用户关联表 Mapper 接口
 */
@Mapper
public interface ClassUserMapper extends BaseMapper<ClassUser> {
}