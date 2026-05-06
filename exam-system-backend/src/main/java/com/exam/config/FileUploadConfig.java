package com.exam.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FileUploadConfig implements WebMvcConfigurer {
    
    @Value("${file.upload-path}")
    private String uploadPath;
    
    @Value("${file.access-path}")
    private String accessPath;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源访问路径
        // 确保路径以 / 结尾
        String normalizedPath = uploadPath.endsWith("/") ? uploadPath : uploadPath + "/";
        
        registry.addResourceHandler(accessPath)
                .addResourceLocations("file:" + normalizedPath);
    }
}
