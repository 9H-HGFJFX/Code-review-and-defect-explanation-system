package com.codereview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codereview.entity.Issue;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审查问题Mapper接口
 */
@Mapper
public interface IssueMapper extends BaseMapper<Issue> {
}
