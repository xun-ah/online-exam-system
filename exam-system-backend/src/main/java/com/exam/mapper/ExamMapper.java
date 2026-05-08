package com.exam.mapper;

import com.exam.entity.Exam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ExamMapper {
    
    Exam selectById(@Param("id") Long id);
    
    List<Exam> selectList(@Param("teacherId") Long teacherId, @Param("classId") Long classId, @Param("status") Integer status);
    
    /**
     * 根据院系ID查询考试列表（通过班级关联院系）
     */
    List<Exam> selectListByDepartmentId(@Param("departmentId") Long departmentId);
    
    /**
     * 根据班级ID查询考试列表
     */
    List<Exam> selectListByClassId(@Param("classId") Long classId);
    
    int insert(Exam exam);
    
    int updateById(Exam exam);
    
    int deleteById(@Param("id") Long id);
    
    int count();
    
    int countByMonth(@Param("year") int year, @Param("month") int month);

    int countByTeacherId(@Param("teacherId") Long teacherId);

    int countOngoingByTeacherId(@Param("teacherId") Long teacherId);
}
