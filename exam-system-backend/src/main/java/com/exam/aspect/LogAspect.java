package com.exam.aspect;

import com.exam.annotation.SysLog;
import com.exam.entity.SystemLog;
import com.exam.mapper.SystemLogMapper;
import com.exam.util.JwtUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 系统日志切面
 */
@Aspect
@Component
public class LogAspect {
    
    @Autowired
    private SystemLogMapper systemLogMapper;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Pointcut("@annotation(com.exam.annotation.SysLog)")
    public void logPointcut() {
    }
    
    @Around("logPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 执行目标方法
        Object result = point.proceed();
        
        long executeTime = System.currentTimeMillis() - startTime;
        
        // 保存日志
        saveLog(point, executeTime);
        
        return result;
    }
    
    private void saveLog(ProceedingJoinPoint joinPoint, long executeTime) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            
            SysLog sysLog = method.getAnnotation(SysLog.class);
            if (sysLog != null) {
                SystemLog log = new SystemLog();
                
                // 获取请求信息
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    
                    // 获取用户信息(从Token中解析)
                    String token = request.getHeader("Authorization");
                    if (token != null && token.startsWith("Bearer ")) {
                        token = token.substring(7);
                        Long userId = jwtUtil.getUserIdFromToken(token);
                        String username = jwtUtil.getUsernameFromToken(token);
                        log.setUserId(userId);
                        log.setUsername(username);
                    }
                    
                    log.setIp(getIpAddress(request));
                    log.setMethod(request.getMethod());
                    log.setParams(getParams(joinPoint));
                }
                
                log.setOperation(sysLog.value());
                log.setCreateTime(LocalDateTime.now());
                
                // 异步保存日志到数据库
                systemLogMapper.insert(log);
            }
        } catch (Exception e) {
            // 日志记录失败不影响主流程
            System.err.println("保存系统日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取请求参数
     */
    private String getParams(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length == 0) {
            return "";
        }
        
        StringBuilder params = new StringBuilder();
        for (Object arg : args) {
            if (arg != null && !isFilterObject(arg)) {
                params.append(arg.toString()).append(" ");
            }
        }
        return params.length() > 500 ? params.substring(0, 500) : params.toString();
    }
    
    /**
     * 判断是否需要过滤的对象
     */
    private boolean isFilterObject(Object obj) {
        return obj instanceof HttpServletRequest || obj instanceof HttpServletResponse;
    }
    
    /**
     * 获取IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
