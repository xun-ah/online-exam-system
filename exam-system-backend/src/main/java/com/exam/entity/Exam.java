package com.exam.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Exam {
    private Long id;
    
    private String examName; // 考试名称
    
    private Long paperId; // 试卷ID
    private String paperName; // 试卷名称（关联查询）
    
    private Long teacherId; // 发布教师ID
    
    private LocalDateTime startTime; // 开始时间
    
    private LocalDateTime endTime; // 结束时间
    
    private Integer duration; // 考试时长（分钟）
    
    private Long classId; // 考试班级ID
    private String className; // 班级名称（关联查询）
    
    private Integer status; // 0-未开始 1-进行中 2-已结束
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    private Integer deleted;
}
