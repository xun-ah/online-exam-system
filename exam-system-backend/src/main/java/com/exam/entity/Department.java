package com.exam.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Department {
    private Long id;
    
    private String deptName; // 院系名称
    
    private String deptCode; // 院系代码
    
    private String description;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    private Integer deleted;
    
    // 前端兼容字段
    private String name;
    
    private String code;
}
