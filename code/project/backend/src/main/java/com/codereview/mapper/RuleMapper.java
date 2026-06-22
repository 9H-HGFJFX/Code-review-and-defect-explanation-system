package com.codereview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codereview.entity.Rule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审查规则Mapper接口
 */
@Mapper
public interface RuleMapper extends BaseMapper<Rule> {
}
