package com.codereview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codereview.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM user WHERE username = #{username} AND is_deleted = 0")
    User selectByUsername(@Param("username") String username);

    /**
     * 根据ID查询用户
     */
    @Select("SELECT * FROM user WHERE id = #{id} AND is_deleted = 0")
    User selectById(@Param("id") Long id);
}
