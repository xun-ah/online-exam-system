package com.exam.controller;

import com.exam.common.Result;
import com.exam.entity.SystemLog;
import com.exam.mapper.SystemLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/admin/logs")
public class AdminLogsController {
    
    @Autowired
    private SystemLogMapper systemLogMapper;
    
    /**
     * 获取系统操作日志
     */
    @GetMapping("/system")
    public Result<Map<String, Object>> getSystemLogs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        
        int offset = (pageNum - 1) * pageSize;
        List<SystemLog> logs = systemLogMapper.selectList(keyword, offset, pageSize);
        int total = systemLogMapper.count(keyword);
        
        // 转换为前端需要的格式
        List<Map<String, Object>> logList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (SystemLog log : logs) {
            Map<String, Object> item = new HashMap<>();
            item.put("time", log.getCreateTime() != null ? log.getCreateTime().format(formatter) : "");
            item.put("operator", log.getUsername());
            item.put("type", log.getMethod());
            item.put("detail", log.getOperation());
            item.put("ip", log.getIp());
            item.put("status", "成功");
            logList.add(item);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", logList);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        
        return Result.success(result);
    }
}
