package com.codeaudit.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeaudit.entity.Rule;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RuleRepository extends BaseMapper<Rule> {
}
