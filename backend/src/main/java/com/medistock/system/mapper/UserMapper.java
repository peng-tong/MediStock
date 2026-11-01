package com.medistock.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medistock.system.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserMapper extends BaseMapper<User> {

    /**
     * 统计同名用户（包含逻辑删除记录）。
     * S5 修复：用户逻辑删除后唯一索引 uk_user_tenant_username 仍占用用户名，
     * 仅按 deleted=0 查重会漏判，插入时撞唯一索引抛异常变成 HTTP 500。
     * 此处显式统计全部记录（租户条件仍由租户拦截器自动拼接），提前返回明确的业务错误。
     */
    @Select("SELECT COUNT(*) FROM sys_user WHERE username = #{username}")
    long countUsernameIncludeDeleted(@Param("username") String username);

    /**
     * 编辑用户时查重：排除自身 id，同样包含逻辑删除记录。
     * 租户条件仍由租户拦截器自动拼接（超管则全租户范围），避免跨租户误判。
     */
    @Select("SELECT COUNT(*) FROM sys_user WHERE username = #{username} AND id <> #{id}")
    long countUsernameIncludeDeletedExcept(@Param("username") String username, @Param("id") Long id);
}
