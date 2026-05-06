package com.exam.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Student {
    private Long id;
    
    private String studentNo; // 学号
    
    private String realName;
    
    private Integer gender; // 0-女 1-男
    
    private String phone;
    
    private String email;
    
    private Long classId;
    
    private Long departmentId;
    
    private String className; // 班级名称（关联查询）
    
    private String departmentName; // 院系名称（关联查询）
    
    private Long userId;
    
    private Integer status; // 用户状态：0-禁用 1-启用
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    private Integer deleted;
}
