package com.exam.controller;

import com.exam.annotation.SysLog;
import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.entity.Teacher;
import com.exam.entity.TeacherClass;
import com.exam.service.TeacherService;
import com.exam.service.TeacherClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/teachers")
public class AdminTeacherController {
    
    @Autowired
    private TeacherService teacherService;
    
    @Autowired
    private TeacherClassService teacherClassService;
    
    @SysLog("查询教师列表")
    @GetMapping
    public Result<PageResult<Teacher>> getTeacherList(
            @RequestParam(required = false) String teacherNo,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<Teacher> result = teacherService.getTeacherList(teacherNo, realName, departmentId, pageNum, pageSize);
        return Result.success(result);
    }
    
    @SysLog("新增教师")
    @PostMapping
    public Result<Void> createTeacher(@RequestBody Teacher teacher) {
        try {
            teacherService.createTeacher(teacher);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @SysLog("更新教师信息")
    @PutMapping("/{id}")
    public Result<Void> updateTeacher(@PathVariable Long id, @RequestBody Teacher teacher) {
        try {
            teacher.setId(id);
            teacherService.updateTeacher(teacher);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @SysLog("删除教师")
    @DeleteMapping("/{id}")
    public Result<Void> deleteTeacher(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
        return Result.success();
    }
    
    @SysLog("查询教师负责的班级")
    @GetMapping("/{id}/classes")
    public Result<List<TeacherClass>> getTeacherClasses(@PathVariable Long id) {
        List<TeacherClass> list = teacherClassService.getClassesByTeacherId(id);
        return Result.success(list);
    }
    
    @SysLog("为教师分配班级")
    @PostMapping("/{id}/assign-classes")
    public Result<Void> assignClasses(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> classIdObjects = (List<Object>) params.get("classIds");
            String subject = params.get("subject") != null ? params.get("subject").toString() : "";
            
            // 将Object列表安全转换为Long列表（处理Integer/Long混用的情况）
            List<Long> classIds = new java.util.ArrayList<>();
            if (classIdObjects != null) {
                for (Object obj : classIdObjects) {
                    if (obj instanceof Number) {
                        classIds.add(((Number) obj).longValue());
                    }
                }
            }
            
            teacherClassService.batchAssignClasses(id, classIds, subject);
            return Result.success("分配成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
