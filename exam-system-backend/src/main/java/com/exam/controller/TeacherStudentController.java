package com.exam.controller;

import com.exam.annotation.SysLog;
import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.entity.Student;
import com.exam.entity.ExamRecord;
import com.exam.entity.Department;
import com.exam.entity.ClassInfo;
import com.exam.mapper.StudentMapper;
import com.exam.mapper.ExamRecordMapper;
import com.exam.mapper.ExamMapper;
import com.exam.mapper.TeacherClassMapper;
import com.exam.mapper.TeacherMapper;
import com.exam.mapper.DepartmentMapper;
import com.exam.mapper.ClassMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 教师端学生管理Controller
 */
@RestController
@RequestMapping("/teacher/students")
public class TeacherStudentController {
    
    @Autowired
    private StudentMapper studentMapper;
    
    @Autowired
    private ExamRecordMapper examRecordMapper;
    
    @Autowired
    private ExamMapper examMapper;
    
    @Autowired
    private TeacherClassMapper teacherClassMapper;
    
    @Autowired
    private TeacherMapper teacherMapper;
    
    @Autowired
    private DepartmentMapper departmentMapper;
    
    @Autowired
    private ClassMapper classMapper;
    
    /**
     * 获取学生列表(教师所属院系的学生)
     */
    @SysLog("查询学生列表")
    @GetMapping
    public Result<PageResult<Map<String, Object>>> getStudentList(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) String studentNo,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestAttribute("userId") Long userId) {
        
        // 获取教师所属院系ID
        var teacher = teacherMapper.selectByUserId(userId);
        if (teacher == null) {
            return Result.error("未找到教师信息");
        }
        
        // 如果没有指定院系，默认使用教师所属院系
        Long targetDepartmentId = departmentId != null ? departmentId : teacher.getDepartmentId();
        
        // 查询本院系的所有学生
        List<Student> allStudents = studentMapper.selectList(studentNo, studentName, classId, targetDepartmentId, 0, 10000);
        
        // 关联查询班级和院系名称
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Student student : allStudents) {
            Map<String, Object> studentMap = new HashMap<>();
            studentMap.put("id", student.getId());
            studentMap.put("studentNo", student.getStudentNo());
            studentMap.put("realName", student.getRealName());
            studentMap.put("gender", student.getGender());
            studentMap.put("phone", student.getPhone());
            studentMap.put("email", student.getEmail());
            studentMap.put("classId", student.getClassId());
            studentMap.put("departmentId", student.getDepartmentId());
            studentMap.put("className", student.getClassName());
            studentMap.put("departmentName", student.getDepartmentName());
            
            resultList.add(studentMap);
        }
        
        // 分页
        int total = resultList.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        
        List<Map<String, Object>> pageList = fromIndex < total ? resultList.subList(fromIndex, toIndex) : new ArrayList<>();
        
        PageResult<Map<String, Object>> pageResult = new PageResult<>((long) total, pageList);
        
        return Result.success(pageResult);
    }
    
    /**
     * 获取学生详情
     */
    @GetMapping("/{id}")
    public Result<Student> getStudentById(@PathVariable Long id) {
        Student student = studentMapper.selectById(id);
        return Result.success(student);
    }
    
    /**
     * 获取学生考试成绩列表
     */
    @GetMapping("/{id}/scores")
    public Result<List<Map<String, Object>>> getStudentScores(@PathVariable Long id) {
        List<Map<String, Object>> scoreList = new ArrayList<>();
        
        // 获取该学生的所有考试记录
        List<ExamRecord> records = examRecordMapper.selectList(null, id);
        
        for (ExamRecord record : records) {
            Map<String, Object> scoreData = new HashMap<>();
            scoreData.put("id", record.getId());
            scoreData.put("examId", record.getExamId());
            scoreData.put("score", record.getScore());
            scoreData.put("submitTime", record.getSubmitTime());
            scoreData.put("status", record.getStatus());
            
            // 获取考试信息
            var exam = examMapper.selectById(record.getExamId());
            if (exam != null) {
                scoreData.put("examName", exam.getExamName());
                // 简化处理:从考试名称中提取科目
                String subject = exam.getExamName();
                if (subject.contains("》")) {
                    subject = subject.substring(1, subject.indexOf("》"));
                }
                scoreData.put("subject", subject);
            }
            
            // TODO: 计算排名(需要查询该考试所有学生的成绩)
            scoreData.put("rank", 0);
            
            scoreList.add(scoreData);
        }
        
        // 按提交时间降序排序
        scoreList.sort((a, b) -> {
            var timeA = (java.time.LocalDateTime) a.get("submitTime");
            var timeB = (java.time.LocalDateTime) b.get("submitTime");
            if (timeA == null && timeB == null) return 0;
            if (timeA == null) return 1;
            if (timeB == null) return -1;
            return timeB.compareTo(timeA);
        });
        
        return Result.success(scoreList);
    }
    
    /**
     * 导出学生成绩
     */
    @SysLog("导出学生成绩")
    @GetMapping("/export")
    public Result<String> exportScores(
            @RequestParam(required = false) Long classId,
            @RequestAttribute("userId") Long userId) {
        // TODO: 实现Excel导出功能
        return Result.success("导出功能开发中");
    }
    
    /**
     * 获取教师所属院系信息
     */
    @GetMapping("/department")
    public Result<Department> getTeacherDepartment(@RequestAttribute("userId") Long userId) {
        // 获取教师信息
        var teacher = teacherMapper.selectByUserId(userId);
        if (teacher == null) {
            return Result.error("未找到教师信息");
        }
        
        // 获取院系信息
        Department department = departmentMapper.selectById(teacher.getDepartmentId());
        if (department == null) {
            return Result.error("未找到院系信息");
        }
        
        return Result.success(department);
    }
    
    /**
     * 获取本院系下的班级列表
     */
    @GetMapping("/classes")
    public Result<List<ClassInfo>> getClassListByDepartment(
            @RequestParam(required = false) Long departmentId,
            @RequestAttribute("userId") Long userId) {
        
        Long targetDepartmentId = departmentId;
        
        // 如果没有指定院系ID，使用教师所属院系
        if (targetDepartmentId == null) {
            var teacher = teacherMapper.selectByUserId(userId);
            if (teacher == null) {
                return Result.error("未找到教师信息");
            }
            targetDepartmentId = teacher.getDepartmentId();
        }
        
        // 查询该院系下的所有班级
        List<ClassInfo> classes = classMapper.selectList(targetDepartmentId);
        
        return Result.success(classes);
    }
}
