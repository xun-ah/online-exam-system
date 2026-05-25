package com.exam.controller;

import com.exam.annotation.SysLog;
import com.exam.common.Result;
import com.exam.entity.Subject;
import com.exam.entity.Teacher;
import com.exam.mapper.TeacherClassMapper;
import com.exam.mapper.TeacherMapper;
import com.exam.service.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/subject")
public class SubjectController {
    
    @Autowired
    private SubjectService subjectService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private TeacherMapper teacherMapper;
    
    @Autowired
    private TeacherClassMapper teacherClassMapper;
    
    /**
     * 获取教师任教的科目列表（用于题库管理等场景）
     */
    @GetMapping("/my-subjects")
    public Result<List<Subject>> getMySubjects(@RequestAttribute("userId") Long userId) {
        // 获取教师信息
        Teacher teacher = teacherMapper.selectByUserId(userId);
        if (teacher == null) {
            return Result.success(new ArrayList<>());
        }
        
        // 查询教师任教的所有科目名称
        List<String> subjectNames = teacherClassMapper.selectDistinctSubjectsByTeacherId(teacher.getId());
        
        if (subjectNames == null || subjectNames.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        
        // 获取本院系所有科目，然后过滤出教师任教的科目
        List<Subject> allSubjects = subjectService.getActiveSubjects(teacher.getDepartmentId());
        List<Subject> mySubjects = allSubjects.stream()
                .filter(s -> subjectNames.contains(s.getName()))
                .collect(Collectors.toList());
        
        return Result.success(mySubjects);
    }
    
    /**
     * 获取教师任教的科目列表（用于题库管理、批量导入等场景）
     */
    @GetMapping("/list")
    public Result<List<Subject>> getSubjectList(@RequestAttribute("userId") Long userId) {
        // 获取教师信息
        Teacher teacher = teacherMapper.selectByUserId(userId);
        if (teacher == null) {
            System.out.println("[SubjectController] 未找到教师信息, userId=" + userId);
            return Result.success(new ArrayList<>());
        }
        
        System.out.println("[SubjectController] 教师信息: id=" + teacher.getId() + ", name=" + teacher.getRealName() + ", deptId=" + teacher.getDepartmentId());
        
        // 查询教师任教的所有科目名称
        List<String> subjectNames = teacherClassMapper.selectDistinctSubjectsByTeacherId(teacher.getId());
        System.out.println("[SubjectController] 教师任教科目: " + subjectNames);
        
        if (subjectNames == null || subjectNames.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        
        // 获取本院系所有科目，然后过滤出教师任教的科目
        List<Subject> allSubjects = subjectService.getActiveSubjects(teacher.getDepartmentId());
        System.out.println("[SubjectController] 本院系所有科目: " + allSubjects.stream().map(Subject::getName).collect(java.util.stream.Collectors.toList()));
        
        List<Subject> mySubjects = allSubjects.stream()
                .filter(s -> subjectNames.contains(s.getName()))
                .collect(Collectors.toList());
        
        System.out.println("[SubjectController] 最终返回科目: " + mySubjects.stream().map(Subject::getName).collect(java.util.stream.Collectors.toList()));
        
        return Result.success(mySubjects);
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
