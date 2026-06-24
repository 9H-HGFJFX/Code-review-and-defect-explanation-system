package com.codereview.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codereview.entity.ClassInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 班级表 Mapper 接口
 */
@Mapper
public interface ClassMapper extends BaseMapper<ClassInfo> {
}