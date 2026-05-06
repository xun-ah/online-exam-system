package com.exam.service;

import com.exam.common.PageResult;
import com.exam.entity.Department;
import com.exam.mapper.DepartmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DepartmentService {
    
    @Autowired
    private DepartmentMapper departmentMapper;
    
    public PageResult<Department> getDepartmentList(int pageNum, int pageSize) {
        List<Department> allList = departmentMapper.selectList();
        
        if (allList == null) {
            allList = new ArrayList<>();
        }
        
        // 分页处理
        int total = allList.size();
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, total);
        
        List<Department> pageData = start < total 
            ? allList.subList(start, end) 
            : new ArrayList<>();
        
        return new PageResult<>((long) total, pageData);
    }
    
    public Department getDepartmentById(Long id) {
        return departmentMapper.selectById(id);
    }
    
    public void createDepartment(Department department) {
        // 检查院系代码是否已存在
        List<Department> allDepts = departmentMapper.selectList();
        if (allDepts != null) {
            for (Department d : allDepts) {
                if (d.getDeptCode() != null && d.getDeptCode().equals(department.getDeptCode())) {
                    throw new RuntimeException("院系代码已存在：" + department.getDeptCode());
                }
                if (d.getDeptName() != null && d.getDeptName().equals(department.getDeptName())) {
                    throw new RuntimeException("院系名称已存在：" + department.getDeptName());
                }
            }
        }
        departmentMapper.insert(department);
    }
    
    public void updateDepartment(Department department) {
        // 检查院系代码和名称是否与其他记录重复
        List<Department> allDepts = departmentMapper.selectList();
        if (allDepts != null) {
            for (Department d : allDepts) {
                if (d.getId().equals(department.getId())) continue; // 跳过自己
                if (d.getDeptCode() != null && d.getDeptCode().equals(department.getDeptCode())) {
                    throw new RuntimeException("院系代码已存在：" + department.getDeptCode());
                }
                if (d.getDeptName() != null && d.getDeptName().equals(department.getDeptName())) {
                    throw new RuntimeException("院系名称已存在：" + department.getDeptName());
                }
            }
        }
        departmentMapper.updateById(department);
    }
    
    public void deleteDepartment(Long id) {
        departmentMapper.deleteById(id);
    }
}
