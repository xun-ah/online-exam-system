package com.exam.mapper;

import com.exam.entity.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface DepartmentMapper {
    
    Department selectById(@Param("id") Long id);
    
    List<Department> selectList();
    
    int insert(Department department);
    
    int updateById(Department department);
    
    int deleteById(@Param("id") Long id);
    
    /**
     * 统计院系考试参与人次
     */
    List<Map<String, Object>> countExamParticipationByDepartment();
}
