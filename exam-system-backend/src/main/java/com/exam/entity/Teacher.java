package com.exam.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Teacher {
    private Long id;
    
    private String teacherNo; // 工号
    
    private String realName;
    
    private Integer gender; // 0-女 1-男
    
    private String phone;
    
    private String email;
    
    private Long departmentId;
    
    private String departmentName; // 院系名称
    
    private Long userId;
    
    private Integer status; // 0-禁用 1-正常
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    private Integer deleted;
}
