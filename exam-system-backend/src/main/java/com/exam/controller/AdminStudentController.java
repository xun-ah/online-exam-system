package com.exam.controller;

import com.exam.annotation.SysLog;
import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.entity.Student;
import com.exam.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/students")
public class AdminStudentController {
    
    @Autowired
    private StudentService studentService;
    
    @SysLog("查询学生列表")
    @GetMapping
    public Result<PageResult<Student>> getStudentList(
            @RequestParam(required = false) String studentNo,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<Student> result = studentService.getStudentList(studentNo, realName, classId, departmentId, pageNum, pageSize);
        return Result.success(result);
    }
    
    @SysLog("新增学生")
    @PostMapping
    public Result<Void> createStudent(@RequestBody Student student) {
        studentService.createStudent(student);
        return Result.success();
    }
    
    @SysLog("更新学生信息")
    @PutMapping("/{id}")
    public Result<Void> updateStudent(@PathVariable Long id, @RequestBody Student student) {
        student.setId(id);
        studentService.updateStudent(student);
        return Result.success();
    }
    
    @SysLog("删除学生")
    @DeleteMapping("/{id}")
    public Result<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return Result.success();
    }
}
