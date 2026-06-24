package com.codereview.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codereview.entity.SubmissionFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 提交文件Mapper接口
 * 
 * @author code-review-team
 */
@Mapper
public interface SubmissionFileMapper extends BaseMapper<SubmissionFile> {

    /**
     * 根据任务ID查询文件列表
     * 
     * @param taskId 任务ID
     * @return 文件列表
     */
    @Select("SELECT * FROM submission_file WHERE task_id = #{taskId} AND deleted = 0 ORDER BY file_path ASC")
    List<SubmissionFile> selectByTaskId(@Param("taskId") Long taskId);

    /**
     * 查询任务中解析失败的文件
     * 
     * @param taskId 任务ID
     * @return 文件列表
     */
    @Select("SELECT * FROM submission_file WHERE task_id = #{taskId} AND status = 'FAILED' AND deleted = 0")
    List<SubmissionFile> selectFailedFilesByTaskId(@Param("taskId") Long taskId);

    /**
     * 批量更新文件状态
     * 
     * @param ids 文件ID列表
     * @param status 新状态
     * @param issueCount 问题数量
     * @return 更新数量
     */
    @Update("<script>" +
            "UPDATE submission_file SET status = #{status}, issue_count = #{issueCount}, update_time = NOW() " +
            "WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}</foreach> AND deleted = 0" +
            "</script>")
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") String status, @Param("issueCount") Integer issueCount);
}
