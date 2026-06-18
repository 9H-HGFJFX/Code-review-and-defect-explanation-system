package com.codeaudit.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeaudit.entity.ClassGroup;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ClassRepository extends BaseMapper<ClassGroup> {
}
