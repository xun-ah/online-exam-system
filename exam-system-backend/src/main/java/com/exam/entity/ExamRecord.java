package com.exam.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ExamRecord {
    private Long id;
    
    private Long examId;
    
    private Long studentId;
    
    private BigDecimal score; // 得分
    
    private String answers; // 学生答案（JSON格式）
    
    private String details; // 答题详情（JSON格式，存储每题得分）
    
    private LocalDateTime submitTime; // 提交时间
    
    private Integer status; // 0-考试中 1-已提交 2-已阅卷
    
    private Integer extraMinutes; // 延长考试时间（分钟）
    
    private Integer switchCount; // 切屏次数
    
    private String questionOrder; // 题目乱序（JSON格式，存储每个学生的固定题目顺序）
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    private Integer deleted;
}
