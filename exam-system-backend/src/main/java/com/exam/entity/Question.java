package com.exam.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Question {
    private Long id;
    
    private String questionNo; // 题目编号
    
    private Integer type; // 1-单选题 2-多选题 3-判断题 4-填空题 5-简答题 6-编程题
    
    private String content; // 题目内容
    
    private String options; // 选项（JSON格式）
    
    private String answer; // 正确答案
    
    private String analysis; // 答案解析
    
    private BigDecimal score; // 分值
    
    private Long teacherId; // 出题教师ID
    
    private String subject; // 所属科目
    
    private String knowledgePoint; // 知识点
    
    private Integer difficulty; // 难度 1-简单 2-中等 3-困难
    
    private String language; // 编程语言（编程题专用）
    
    private String codeTemplate; // 代码模板（编程题专用）
    
    private String testCases; // 测试用例（JSON格式，编程题专用）
    
    private Integer timeLimit; // 时间限制（毫秒，编程题专用）
    
    private Integer memoryLimit; // 内存限制（MB，编程题专用）
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    private Integer deleted;
}
