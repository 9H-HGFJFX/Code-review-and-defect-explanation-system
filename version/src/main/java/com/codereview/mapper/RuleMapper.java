package com.codereview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codereview.entity.Rule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 规则Mapper
 */
@Mapper
public interface RuleMapper extends BaseMapper<Rule> {

    /**
     * 根据ID查询规则
     */
    @Select("SELECT * FROM rule WHERE id = #{id}")
    Rule selectById(@Param("id") Long id);
}
