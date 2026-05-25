package com.exam.controller;

import com.exam.annotation.SysLog;
import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.entity.Department;
import com.exam.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/departments")
public class DepartmentController {
    
    @Autowired
    private DepartmentService departmentService;
    
    @GetMapping
    public Result<PageResult<Department>> getDepartmentList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<Department> result = departmentService.getDepartmentList(pageNum, pageSize);
        // 设置前端兼容字段
        if (result.getRecords() != null) {
            result.getRecords().forEach(dept -> {
                dept.setName(dept.getDeptName());
                dept.setCode(dept.getDeptCode());
            });
        }
        return Result.success(result);
    }
    
    @GetMapping("/{id}")
    public Result<Department> getDepartmentById(@PathVariable Long id) {
        Department department = departmentService.getDepartmentById(id);
        return Result.success(department);
    }
    
    @SysLog("新增院系")
    @PostMapping
    public Result<Void> createDepartment(@RequestBody Department department) {
        try {
            // 将前端字段转换为后端字段
            department.setDeptName(department.getName());
            department.setDeptCode(department.getCode());
            departmentService.createDepartment(department);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @SysLog("更新院系信息")
    @PutMapping("/{id}")
    public Result<Void> updateDepartment(@PathVariable Long id, @RequestBody Department department) {
        try {
            department.setId(id);
            // 将前端字段转换为后端字段
            if (department.getName() != null) {
                department.setDeptName(department.getName());
            }
            if (department.getCode() != null) {
                department.setDeptCode(department.getCode());
            }
            departmentService.updateDepartment(department);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @SysLog("删除院系")
    @DeleteMapping("/{id}")
    public Result<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return Result.success();
    }
}
