package com.exam.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ClassInfo {
    private Long id;
    
    private String className; // 班级名称
    
    private String classCode; // 班级代码
    
    private Long departmentId;
    
    private String departmentName; // 院系名称
    
    private String grade; // 年级
    
    private String description;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    private Integer deleted;
}
