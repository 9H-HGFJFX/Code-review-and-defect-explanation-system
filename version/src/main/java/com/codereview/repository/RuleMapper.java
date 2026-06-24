package com.codereview.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codereview.entity.Rule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 规则表 Mapper 接口
 */
@Mapper
public interface RuleMapper extends BaseMapper<Rule> {
}