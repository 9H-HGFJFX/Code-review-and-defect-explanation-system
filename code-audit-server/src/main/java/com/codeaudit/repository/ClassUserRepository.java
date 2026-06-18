package com.codeaudit.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeaudit.entity.ClassUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ClassUserRepository extends BaseMapper<ClassUser> {
}
