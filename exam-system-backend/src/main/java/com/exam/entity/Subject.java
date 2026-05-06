package com.exam.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Subject {
    private Long id;
    
    private Long departmentId; // 院系ID
    
    private String name; // 科目名称
    
    private String code; // 科目代码
    
    private String description; // 科目描述
    
    private Integer credits; // 学分
    
    private String departmentName; // 院系名称（用于显示）
    
    private Integer status; // 状态 0-禁用 1-启用
    
    private Integer sortOrder; // 排序顺序
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    private Integer deleted;
}
