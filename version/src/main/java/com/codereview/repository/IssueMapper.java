package com.codereview.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codereview.entity.Issue;
import org.apache.ibatis.annotations.Mapper;

/**
 * 代码缺陷表 Mapper 接口
 */
@Mapper
public interface IssueMapper extends BaseMapper<Issue> {
}