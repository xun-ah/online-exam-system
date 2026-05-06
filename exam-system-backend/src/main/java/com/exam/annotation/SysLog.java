package com.exam.annotation;

import java.lang.annotation.*;

/**
 * 系统日志注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SysLog {
    
    /**
     * 操作描述
     */
    String value() default "";
    
    /**
     * 操作类型
     */
    String type() default "OTHER";
}
