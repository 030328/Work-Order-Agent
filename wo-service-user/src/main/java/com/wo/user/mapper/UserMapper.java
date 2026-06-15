package com.wo.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wo.user.entity.SysUser;
import org.apache.ibatis.annotations.Select;

public interface UserMapper extends BaseMapper<SysUser> {

    /**
     * Find user by username
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    SysUser selectByUsername(String username);
}
