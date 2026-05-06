package com.exam.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Paper {
    private Long id;
    
    private String paperName; // 试卷名称
    
    private String paperNo; // 试卷编号
    
    private BigDecimal totalScore; // 总分
    
    private Integer duration; // 考试时长（分钟）
    
    private Long teacherId; // 创建教师ID
    
    private String description;
    
    private String subject; // 科目
    
    private Integer difficulty; // 难度 1-易 2-中 3-难
    
    private String status; // 状态 published-已发布 unpublished-未发布 ended-已结束
    
    private String questionConfig; // 题型配置
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    private Integer deleted;
}
