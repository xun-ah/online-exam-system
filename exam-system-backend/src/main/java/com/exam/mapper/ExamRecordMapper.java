package com.exam.mapper;

import com.exam.entity.ExamRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ExamRecordMapper {
    
    ExamRecord selectById(@Param("id") Long id);
    
    List<ExamRecord> selectList(@Param("examId") Long examId, @Param("studentId") Long studentId);
    
    int insert(ExamRecord examRecord);
    
    int updateById(ExamRecord examRecord);
    
    int deleteById(@Param("id") Long id);
    
    int countParticipants();
    
    int countByMonth(@Param("year") int year, @Param("month") int month);
}
