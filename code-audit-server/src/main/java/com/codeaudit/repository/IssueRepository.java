package com.codeaudit.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeaudit.entity.Issue;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IssueRepository extends BaseMapper<Issue> {
}
