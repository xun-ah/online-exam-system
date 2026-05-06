package com.exam.service;

import com.exam.mapper.SystemLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 系统日志定时清理服务
 */
@Service
public class SystemLogCleanupService {
    
    private static final Logger log = LoggerFactory.getLogger(SystemLogCleanupService.class);
    
    @Autowired
    private SystemLogMapper systemLogMapper;
    
    /**
     * 每天凌晨2点执行，清理7天前的日志
     * cron表达式：秒 分 时 日 月 周
     * 0 0 2 * * ? 表示每天凌晨2点
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldLogs() {
        log.info("开始执行系统日志清理任务...");
        try {
            int deletedCount = systemLogMapper.deleteByDaysAgo(7);
            log.info("系统日志清理完成，共删除 {} 条7天前的日志记录", deletedCount);
        } catch (Exception e) {
            log.error("系统日志清理任务执行失败", e);
        }
    }
}
