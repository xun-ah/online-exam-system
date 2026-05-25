package com.exam.controller;

import com.exam.annotation.SysLog;
import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.entity.ClassInfo;
import com.exam.service.ClassInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/classes")
public class ClassInfoController {
    
    @Autowired
    private ClassInfoService classInfoService;
    
    @GetMapping
    public Result<PageResult<ClassInfo>> getClassList(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<ClassInfo> result = classInfoService.getClassList(departmentId, pageNum, pageSize);
        return Result.success(result);
    }
    
    @GetMapping("/{id}")
    public Result<ClassInfo> getClassById(@PathVariable Long id) {
        ClassInfo classInfo = classInfoService.getClassById(id);
        return Result.success(classInfo);
    }
    
    @SysLog("新增班级")
    @PostMapping
    public Result<Void> createClass(@RequestBody ClassInfo classInfo) {
        try {
            classInfoService.createClass(classInfo);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @SysLog("更新班级信息")
    @PutMapping("/{id}")
    public Result<Void> updateClass(@PathVariable Long id, @RequestBody ClassInfo classInfo) {
        try {
            classInfo.setId(id);
            classInfoService.updateClass(classInfo);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @SysLog("删除班级")
    @DeleteMapping("/{id}")
    public Result<Void> deleteClass(@PathVariable Long id) {
        classInfoService.deleteClass(id);
        return Result.success();
    }
}
