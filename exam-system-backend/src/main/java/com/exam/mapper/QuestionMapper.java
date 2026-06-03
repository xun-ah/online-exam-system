package com.exam.mapper;

import com.exam.entity.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface QuestionMapper {
    
    Question selectById(@Param("id") Long id);
    
    List<Question> selectList(@Param("teacherId") Long teacherId,
                              @Param("type") Integer type,
                              @Param("difficulty") Integer difficulty,
                              @Param("keyword") String keyword,
                              @Param("subject") String subject,
                              @Param("offset") int offset,
                              @Param("limit") int limit);
    
    int insert(Question question);
    
    int updateById(Question question);
    
    int deleteById(@Param("id") Long id);
    
    int count(@Param("teacherId") Long teacherId,
              @Param("type") Integer type,
              @Param("difficulty") Integer difficulty,
              @Param("keyword") String keyword,
              @Param("subject") String subject);
    
    /**
     * 根据条件查询题目列表（无分页，用于导出）
     */
    List<Question> selectListByCondition(@Param("teacherId") Long teacherId,
                                         @Param("type") Integer type,
                                         @Param("difficulty") Integer difficulty,
                                         @Param("keyword") String keyword,
                                         @Param("subject") String subject);
    
    int countByTeacherId(@Param("teacherId") Long teacherId);
    
    /**
     * 统计本月新增题目数
     */
    int countByTeacherIdAndMonth(@Param("teacherId") Long teacherId);
    
    /**
     * 统计覆盖科目数
     */
    int countDistinctSubjectByTeacherId(@Param("teacherId") Long teacherId);
    
    /**
     * 统计各题型数量
     */
    List<Map<String, Object>> countByType();
    
    /**
     * 统计月度考试趋势
     */
    List<Map<String, Object>> countMonthlyTrend();
}
