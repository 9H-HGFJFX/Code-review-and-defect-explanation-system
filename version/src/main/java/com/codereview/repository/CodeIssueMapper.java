package com.codereview.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codereview.entity.CodeIssue;
import com.codereview.common.enums.IssueSeverity;
import com.codereview.common.enums.IssueStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 代码缺陷Mapper接口
 * 
 * @author code-review-team
 */
@Mapper
public interface CodeIssueMapper extends BaseMapper<CodeIssue> {

    /**
     * 根据任务ID查询问题列表
     * 
     * @param taskId 任务ID
     * @return 问题列表
     */
    @Select("SELECT * FROM code_issue WHERE task_id = #{taskId} AND deleted = 0 ORDER BY severity ASC, line_number ASC")
    List<CodeIssue> selectByTaskId(@Param("taskId") Long taskId);

    /**
     * 根据任务ID和严重程度查询问题
     * 
     * @param taskId 任务ID
     * @param severity 严重程度
     * @return 问题列表
     */
    @Select("SELECT * FROM code_issue WHERE task_id = #{taskId} AND severity = #{severity} AND deleted = 0 ORDER BY line_number ASC")
    List<CodeIssue> selectByTaskIdAndSeverity(@Param("taskId") Long taskId, @Param("severity") IssueSeverity severity);

    /**
     * 批量更新问题状态
     * 
     * @param ids 问题ID列表
     * @param status 新状态
     * @return 更新数量
     */
    @Update("<script>" +
            "UPDATE code_issue SET status = #{status}, update_time = NOW() " +
            "WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}</foreach> AND deleted = 0" +
            "</script>")
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") IssueStatus status);

    /**
     * 查询待分配的问题
     * 
     * @param taskId 任务ID
     * @return 问题列表
     */
    @Select("SELECT * FROM code_issue WHERE task_id = #{taskId} AND assignee_id IS NULL AND deleted = 0 ORDER BY severity ASC")
    List<CodeIssue> selectUnassignedByTaskId(@Param("taskId") Long taskId);
}
