package com.codereview.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codereview.entity.ReviewRule;
import com.codereview.common.enums.RuleCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 审查规则Mapper接口
 * 
 * @author code-review-team
 */
@Mapper
public interface ReviewRuleMapper extends BaseMapper<ReviewRule> {

    /**
     * 查询所有启用的规则
     * 
     * @return 规则列表
     */
    @Select("SELECT * FROM review_rule WHERE enabled = TRUE AND deleted = 0 ORDER BY priority ASC, category ASC")
    List<ReviewRule> selectAllEnabled();

    /**
     * 根据类别查询规则
     * 
     * @param category 规则类别
     * @return 规则列表
     */
    @Select("SELECT * FROM review_rule WHERE category = #{category} AND enabled = TRUE AND deleted = 0 ORDER BY priority ASC")
    List<ReviewRule> selectByCategory(@Param("category") RuleCategory category);

    /**
     * 根据语言查询规则
     * 
     * @param language 编程语言
     * @return 规则列表
     */
    @Select("SELECT * FROM review_rule WHERE languages LIKE CONCAT('%', #{language}, '%') AND enabled = TRUE AND deleted = 0 ORDER BY priority ASC")
    List<ReviewRule> selectByLanguage(@Param("language") String language);

    /**
     * 批量启用/禁用规则
     * 
     * @param ids 规则ID列表
     * @param enabled 是否启用
     * @return 更新数量
     */
    @Update("<script>" +
            "UPDATE review_rule SET enabled = #{enabled}, update_time = NOW() " +
            "WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}</foreach> AND deleted = 0" +
            "</script>")
    int batchUpdateEnabled(@Param("ids") List<Long> ids, @Param("enabled") Boolean enabled);

    /**
     * 根据规则标识查询
     * 
     * @param ruleId 规则标识
     * @return 规则
     */
    @Select("SELECT * FROM review_rule WHERE rule_id = #{ruleId} AND deleted = 0 LIMIT 1")
    ReviewRule selectByRuleId(@Param("ruleId") String ruleId);
}
