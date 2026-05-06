package com.exam.mapper;

import com.exam.entity.ClassInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ClassMapper {
    
    ClassInfo selectById(@Param("id") Long id);
    
    List<ClassInfo> selectList(@Param("departmentId") Long departmentId);
    
    int insert(ClassInfo classInfo);
    
    int updateById(ClassInfo classInfo);
    
    int deleteById(@Param("id") Long id);
}
