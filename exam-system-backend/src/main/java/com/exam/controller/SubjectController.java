package com.exam.controller;

import com.exam.annotation.SysLog;
import com.exam.common.Result;
import com.exam.entity.Subject;
import com.exam.service.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subject")
public class SubjectController {
    
    @Autowired
    private SubjectService subjectService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 获取教师所属院系的科目列表
     */
    @GetMapping("/list")
    public Result<List<Subject>> getSubjectList(@RequestAttribute("userId") Long userId) {
        // TODO: 根据userId查询教师所属院系ID
        // 这里简化处理，需要查询teacher表获取department_id
        Long departmentId = getDepartmentIdByUserId(userId);
        List<Subject> subjects = subjectService.getActiveSubjects(departmentId);
        return Result.success(subjects);
    }
    
    /**
     * 获取所有科目（包括禁用的，管理员使用）
     */
    @GetMapping("/all")
    public Result<List<Subject>> getAllSubjects() {
        List<Subject> subjects = subjectService.getAllSubjects();
        return Result.success(subjects);
    }
    
    /**
     * 管理员获取科目列表（分页）
     */
    @GetMapping("/list/admin")
    public Result<com.exam.common.PageResult<Subject>> getAdminSubjectList(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        com.exam.common.PageResult<Subject> result = subjectService.getSubjectList(name, departmentId, pageNum, pageSize);
        return Result.success(result);
    }
    
    /**
     * 新增科目（管理员使用）
     */
    @SysLog("新增科目")
    @PostMapping
    public Result<Void> createSubject(@RequestBody Subject subject) {
        subjectService.createSubject(subject);
        return Result.success();
    }
    
    /**
     * 更新科目（管理员使用）
     */
    @SysLog("更新科目")
    @PutMapping("/{id}")
    public Result<Void> updateSubject(@PathVariable Long id, @RequestBody Subject subject) {
        subject.setId(id);
        subjectService.updateSubject(subject);
        return Result.success();
    }
    
    /**
     * 删除科目（管理员使用）
     */
    @SysLog("删除科目")
    @DeleteMapping("/{id}")
    public Result<Void> deleteSubject(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return Result.success();
    }
    
    /**
     * 根据userId获取教师的院系ID
     */
    private Long getDepartmentIdByUserId(Long userId) {
        try {
            String sql = "SELECT department_id FROM teacher WHERE user_id = ? AND deleted = 0";
            Long departmentId = jdbcTemplate.queryForObject(sql, Long.class, userId);
            if (departmentId != null) {
                return departmentId;
            }
        } catch (Exception e) {
            // 查询失败，返回默认值
        }
        return 1L; // 默认返回计算机学院
    }
}
