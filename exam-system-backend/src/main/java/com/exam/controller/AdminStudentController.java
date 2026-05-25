package com.exam.controller;

import com.exam.annotation.SysLog;
import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.entity.Student;
import com.exam.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/admin/students")
public class AdminStudentController {
    
    @Autowired
    private StudentService studentService;
    
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
    
    /**
     * Excel导入学生
     */
    @SysLog("Excel导入学生")
    @PostMapping("/import")
    public Result<String> importStudents(@RequestParam("file") MultipartFile file,
                                         HttpServletRequest request) {
        try {
            String result = studentService.importStudentsFromExcel(file);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("导入失败：" + e.getMessage());
        }
    }
    
    /**
     * 下载导入模板
     */
    @GetMapping("/template")
    public void downloadTemplate(HttpServletRequest request, javax.servlet.http.HttpServletResponse response) {
        try {
            studentService.downloadTemplate(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 获取导入结果统计
     */
    private String getImportResultMessage(int successCount, int failCount, List<String> errors) {
        StringBuilder msg = new StringBuilder();
        msg.append("导入完成！成功：").append(successCount).append("条，失败：").append(failCount).append("条");
        if (!errors.isEmpty()) {
            msg.append("\n失败原因：");
            for (String error : errors) {
                msg.append("\n-").append(error);
            }
        }
        return msg.toString();
    }
}
