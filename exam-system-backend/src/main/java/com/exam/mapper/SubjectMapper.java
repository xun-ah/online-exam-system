package com.exam.mapper;

import com.exam.entity.Subject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SubjectMapper {
    
    /**
     * 查询所有启用的科目列表
     */
    List<Subject> selectActiveList(@Param("departmentId") Long departmentId);
    
    /**
     * 根据ID查询科目
     */
    Subject selectById(@Param("id") Long id);
    
    /**
     * 查询所有科目（包括禁用的）
     */
    List<Subject> selectAll();
    
    /**
     * 新增科目
     */
    int insert(Subject subject);
    
    /**
     * 更新科目
     */
    int update(Subject subject);
    
    /**
     * 删除科目（逻辑删除）
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 管理员分页查询科目列表
     */
    List<Subject> selectList(@Param("name") String name, 
                            @Param("departmentId") Long departmentId,
                            @Param("offset") int offset,
                            @Param("pageSize") int pageSize);
    
    /**
     * 统计科目数量
     */
    int count(@Param("name") String name, @Param("departmentId") Long departmentId);
}
