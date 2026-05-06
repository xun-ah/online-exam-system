package com.exam.service;

import com.exam.common.PageResult;
import com.exam.entity.ClassInfo;
import com.exam.mapper.ClassMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClassInfoService {
    
    @Autowired
    private ClassMapper classMapper;
    
    public PageResult<ClassInfo> getClassList(Long departmentId, int pageNum, int pageSize) {
        List<ClassInfo> allList = classMapper.selectList(departmentId);
        
        if (allList == null) {
            allList = new ArrayList<>();
        }
        
        // 分页处理
        int total = allList.size();
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, total);
        
        List<ClassInfo> pageData = start < total 
            ? allList.subList(start, end) 
            : new ArrayList<>();
        
        return new PageResult<>((long) total, pageData);
    }
    
    public ClassInfo getClassById(Long id) {
        return classMapper.selectById(id);
    }
    
    public void createClass(ClassInfo classInfo) {
        // 检查班级代码和名称是否已存在
        List<ClassInfo> allClasses = classMapper.selectList(null);
        if (allClasses != null) {
            for (ClassInfo c : allClasses) {
                if (c.getClassCode() != null && c.getClassCode().equals(classInfo.getClassCode())) {
                    throw new RuntimeException("班级代码已存在：" + classInfo.getClassCode());
                }
                if (c.getClassName() != null && c.getClassName().equals(classInfo.getClassName())) {
                    throw new RuntimeException("班级名称已存在：" + classInfo.getClassName());
                }
            }
        }
        classMapper.insert(classInfo);
    }
    
    public void updateClass(ClassInfo classInfo) {
        // 检查班级代码和名称是否与其他记录重复
        List<ClassInfo> allClasses = classMapper.selectList(null);
        if (allClasses != null) {
            for (ClassInfo c : allClasses) {
                if (c.getId().equals(classInfo.getId())) continue; // 跳过自己
                if (c.getClassCode() != null && c.getClassCode().equals(classInfo.getClassCode())) {
                    throw new RuntimeException("班级代码已存在：" + classInfo.getClassCode());
                }
                if (c.getClassName() != null && c.getClassName().equals(classInfo.getClassName())) {
                    throw new RuntimeException("班级名称已存在：" + classInfo.getClassName());
                }
            }
        }
        classMapper.updateById(classInfo);
    }
    
    public void deleteClass(Long id) {
        classMapper.deleteById(id);
    }
}
