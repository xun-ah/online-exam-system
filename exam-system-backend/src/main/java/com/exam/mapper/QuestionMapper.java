package com.exam.mapper;

import com.exam.entity.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

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
    
    int countByTeacherId(@Param("teacherId") Long teacherId);
}
