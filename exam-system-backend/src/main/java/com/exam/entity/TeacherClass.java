package com.exam.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TeacherClass {
    private Long id;
    
    private Long teacherId; // 教师ID
    
    private Long classId; // 班级ID
    
    private String className; // 班级名称（用于显示）
    
    private Long subjectId; // 任教科目ID
    
    private String subject; // 任教科目名称（关联查询）
    
    // 添加subject字段的setter方法,确保MyBatis能正确映射
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    private LocalDateTime createTime;
}
