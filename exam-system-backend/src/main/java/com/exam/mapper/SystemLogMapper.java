package com.exam.mapper;

import com.exam.entity.SystemLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface SystemLogMapper {
    
    List<SystemLog> selectList(@Param("keyword") String keyword, 
                                @Param("offset") int offset, 
                                @Param("limit") int limit);
    
    int count(@Param("keyword") String keyword);
    
    int insert(SystemLog log);
    
    /**
     * 删除指定天数之前的日志
     */
    int deleteByDaysAgo(@Param("days") int days);
}
