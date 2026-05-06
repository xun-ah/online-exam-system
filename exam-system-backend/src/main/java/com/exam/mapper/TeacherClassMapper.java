package com.exam.mapper;

import com.exam.entity.TeacherClass;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface TeacherClassMapper {
    
    /**
     * 查询教师负责的班级列表
     */
    List<TeacherClass> selectByTeacherId(@Param("teacherId") Long teacherId);
    
    /**
     * 查询班级的任课教师列表
     */
    List<TeacherClass> selectByClassId(@Param("classId") Long classId);
    
    /**
     * 添加教师班级关联
     */
    int insert(TeacherClass teacherClass);
    
    /**
     * 批量添加教师班级关联
     */
    int batchInsert(@Param("list") List<TeacherClass> list);
    
    /**
     * 删除教师班级关联
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 删除教师的所有班级关联
     */
    int deleteByTeacherId(@Param("teacherId") Long teacherId);
    
    /**
     * 删除班级的所有教师关联
     */
    int deleteByClassId(@Param("classId") Long classId);
    
    /**
     * 检查教师是否负责某个班级
     */
    TeacherClass selectByTeacherAndClass(@Param("teacherId") Long teacherId, @Param("classId") Long classId);
}
