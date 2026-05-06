package com.exam.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SystemLog {
    private Long id;
    
    private Long userId; // 用户ID
    
    private String username; // 用户名
    
    private String operation; // 操作内容
    
    private String method; // 请求方法
    
    private String params; // 请求参数
    
    private String ip; // IP地址
    
    private LocalDateTime createTime; // 创建时间
}
