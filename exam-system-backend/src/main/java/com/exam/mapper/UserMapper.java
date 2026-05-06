package com.exam.mapper;

import com.exam.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserMapper {
    
    User selectById(@Param("id") Long id);
    
    User selectByUsername(@Param("username") String username);
    
    List<User> selectList(@Param("role") Integer role, @Param("status") Integer status);
    
    int insert(User user);
    
    int updateById(User user);
    
    int deleteById(@Param("id") Long id);
}
