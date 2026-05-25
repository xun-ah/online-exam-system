package com.exam.mapper;

import com.exam.entity.Teacher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface TeacherMapper {
    
    Teacher selectById(@Param("id") Long id);
    
    /**
     * 根据userId查询教师信息
     */
    Teacher selectByUserId(@Param("userId") Long userId);
    
    List<Teacher> selectList(@Param("teacherNo") String teacherNo, 
                            @Param("realName") String realName,
                            @Param("departmentId") Long departmentId,
                            @Param("offset") int offset,
                            @Param("limit") int pageSize);
    
    int insert(Teacher teacher);
    
    int updateById(Teacher teacher);
    
    int deleteById(@Param("id") Long id);
    
    int count(@Param("teacherNo") String teacherNo,
             @Param("realName") String realName,
             @Param("departmentId") Long departmentId);
    
    int countByMonth(@Param("year") int year, @Param("month") int month);
    
    int countParticipants();
    
    int deleteByUserId(@Param("userId") Long userId);
    
    Teacher selectByTeacherNo(@Param("teacherNo") String teacherNo);
    
    /**
     * 根据教师ID查询授课班级数量
     */
    int countClassesByTeacherId(@Param("teacherId") Long teacherId);
    
    /**
     * 根据教师ID查询覆盖学生数量
     */
    int countStudentsByTeacherId(@Param("teacherId") Long teacherId);
    
    /**
     * 根据教师ID查询任教科目数量
     */
    int countSubjectsByTeacherId(@Param("teacherId") Long teacherId);
}
