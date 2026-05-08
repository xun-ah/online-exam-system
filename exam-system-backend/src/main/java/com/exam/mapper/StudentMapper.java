package com.exam.mapper;

import com.exam.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface StudentMapper {
    
    Student selectById(@Param("id") Long id);
    
    /**
     * 根据userId查询学生信息
     */
    Student selectByUserId(@Param("userId") Long userId);
    
    List<Student> selectList(@Param("studentNo") String studentNo,
                             @Param("realName") String realName,
                             @Param("classId") Long classId,
                             @Param("departmentId") Long departmentId,
                             @Param("offset") int offset,
                             @Param("limit") int limit);
    
    int insert(Student student);
    
    int updateById(Student student);
    
    int deleteById(@Param("id") Long id);
    
    int count(@Param("studentNo") String studentNo,
              @Param("realName") String realName,
              @Param("classId") Long classId,
              @Param("departmentId") Long departmentId);
    
    int countByMonth(@Param("year") int year, @Param("month") int month);
}
