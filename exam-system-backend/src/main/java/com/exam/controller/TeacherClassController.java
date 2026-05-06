package com.exam.controller;

import com.exam.common.Result;
import com.exam.entity.TeacherClass;
import com.exam.service.TeacherClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher-class")
public class TeacherClassController {
    
    @Autowired
    private TeacherClassService teacherClassService;
    
    /**
     * 查询教师负责的班级列表
     */
    @GetMapping("/teacher/{teacherId}")
    public Result<List<TeacherClass>> getClassesByTeacherId(@PathVariable Long teacherId) {
        List<TeacherClass> list = teacherClassService.getClassesByTeacherId(teacherId);
        return Result.success(list);
    }
    
    /**
     * 查询班级的任课教师列表
     */
    @GetMapping("/class/{classId}")
    public Result<List<TeacherClass>> getTeachersByClassId(@PathVariable Long classId) {
        List<TeacherClass> list = teacherClassService.getTeachersByClassId(classId);
        return Result.success(list);
    }
    
    /**
     * 为教师分配班级（单个）
     */
    @PostMapping("/assign")
    public Result<Void> assignClassToTeacher(@RequestBody TeacherClass teacherClass) {
        try {
            teacherClassService.assignClassToTeacher(teacherClass);
            return Result.success("分配成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 批量为教师分配班级
     */
    @PostMapping("/batch-assign")
    public Result<Void> batchAssignClasses(@RequestBody Map<String, Object> params) {
        try {
            Long teacherId = Long.valueOf(params.get("teacherId").toString());
            @SuppressWarnings("unchecked")
            List<Long> classIds = (List<Long>) params.get("classIds");
            String subject = params.get("subject") != null ? params.get("subject").toString() : "";
            
            teacherClassService.batchAssignClasses(teacherId, classIds, subject);
            return Result.success("批量分配成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 移除教师的班级
     */
    @DeleteMapping("/{id}")
    public Result<Void> removeClassFromTeacher(@PathVariable Long id) {
        teacherClassService.removeClassFromTeacher(id);
        return Result.success("移除成功", null);
    }
    
    /**
     * 检查教师是否负责某个班级
     */
    @GetMapping("/check")
    public Result<Boolean> checkTeacherClass(
            @RequestParam Long teacherId, 
            @RequestParam Long classId) {
        boolean result = teacherClassService.isTeacherResponsibleForClass(teacherId, classId);
        return Result.success(result);
    }
}
