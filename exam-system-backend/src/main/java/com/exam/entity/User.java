package com.exam.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    
    private String username;
    
    private String password;
    
    private String realName;
    
    private Integer role; // 1-管理员 2-教师 3-学生
    
    private String phone;
    
    private String email;
    
    private String avatar; // 头像URL
    
    private Integer status; // 0-禁用 1-启用
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    private Integer deleted;
}
