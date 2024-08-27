package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    /**
     * 根据openid查询用户
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenid (String openid);

    /**
     * 向用户表中插入新数据
     * @param user
     */
    void insert(User user);   // 这里需要返回主键值

    @Select("select * from user where id = #{id}")
    User getById(Long id);
}
