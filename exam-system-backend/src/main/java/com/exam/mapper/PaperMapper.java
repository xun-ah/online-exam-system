package com.exam.mapper;

import com.exam.entity.Paper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface PaperMapper {
    
    Paper selectById(@Param("id") Long id);
    
    List<Paper> selectList(@Param("teacherId") Long teacherId);
    
    /**
     * 根据院系ID查询试卷列表（通过教师关联院系）
     */
    List<Paper> selectListByDepartmentId(@Param("departmentId") Long departmentId);
    
    int insert(Paper paper);
    
    int updateById(Paper paper);
    
    int deleteById(@Param("id") Long id);

    int countByTeacherId(@Param("teacherId") Long teacherId);
}
