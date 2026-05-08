package com.exam.controller;

import com.exam.annotation.SysLog;
import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.entity.Exam;
import com.exam.entity.ExamRecord;
import com.exam.entity.Teacher;
import com.exam.entity.TeacherClass;
import com.exam.mapper.ExamRecordMapper;
import com.exam.mapper.StudentMapper;
import com.exam.mapper.TeacherMapper;
import com.exam.service.ExamService;
import com.exam.service.TeacherClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/teacher/exams")
public class TeacherExamController {
    
    @Autowired
    private ExamService examService;
    
    @Autowired
    private TeacherClassService teacherClassService;
    
    @Autowired
    private TeacherMapper teacherMapper;
    
    @Autowired
    private ExamRecordMapper examRecordMapper;
    
    @Autowired
    private StudentMapper studentMapper;
    
    /**
     * 获取考试列表（只返回本院系的考试）
     */
    @SysLog("查询考试列表")
    @GetMapping
    public Result<List<Map<String, Object>>> getExamList(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestAttribute("userId") Long userId) {
        // 获取教师所属院系ID
        Teacher teacher = teacherMapper.selectByUserId(userId);
        if (teacher == null) {
            return Result.error("未找到教师信息");
        }
        
        PageResult<Exam> result = examService.getExamListByDepartment(teacher.getDepartmentId(), pageNum, pageSize);
        
        // 为每个考试添加统计信息
        List<Map<String, Object>> examList = result.getRecords().stream()
            .map(exam -> {
                Map<String, Object> examData = new HashMap<>();
                examData.put("id", exam.getId());
                examData.put("examName", exam.getExamName());
                examData.put("className", exam.getClassName());
                examData.put("paperName", exam.getPaperName());
                examData.put("startTime", exam.getStartTime());
                examData.put("endTime", exam.getEndTime());
                examData.put("duration", exam.getDuration());
                examData.put("status", exam.getStatus());
                
                // 统计已完成人数（status >= 1）
                List<ExamRecord> records = examRecordMapper.selectList(exam.getId(), null);
                long completedCount = records.stream()
                    .filter(r -> r.getStatus() != null && r.getStatus() >= 1)
                    .count();
                examData.put("participantCount", completedCount);
                
                // 统计班级总人数
                int totalCount = studentMapper.count(null, null, exam.getClassId(), teacher.getDepartmentId());
                examData.put("totalCount", totalCount);
                
                return examData;
            })
            .collect(Collectors.toList());
        
        return Result.success(examList);
    }
    
    /**
     * 获取考试详情
     */
    @GetMapping("/{id}")
    public Result<Exam> getExamById(@PathVariable Long id) {
        Exam exam = examService.getExamById(id);
        if (exam == null) {
            return Result.error("考试不存在");
        }
        return Result.success(exam);
    }
    
    /**
     * 创建考试
     */
    @SysLog("创建考试")
    @PostMapping
    public Result<Void> createExam(@RequestBody Exam exam, @RequestAttribute("userId") Long userId) {
        // 通过userId查询教师信息
        Teacher teacher = teacherMapper.selectByUserId(userId);
        if (teacher != null) {
            exam.setTeacherId(teacher.getId());
        } else {
            return Result.error("未找到教师信息");
        }
        
        examService.createExam(exam, userId);
        return Result.success("创建成功", null);
    }
    
    /**
     * 更新考试
     */
    @SysLog("更新考试")
    @PutMapping("/{id}")
    public Result<Void> updateExam(@PathVariable Long id, @RequestBody Exam exam) {
        exam.setId(id);
        examService.updateExam(exam);
        return Result.success("更新成功", null);
    }
    
    /**
     * 删除考试
     */
    @SysLog("删除考试")
    @DeleteMapping("/{id}")
    public Result<Void> deleteExam(@PathVariable Long id) {
        examService.deleteExam(id);
        return Result.success("删除成功", null);
    }
    
    /**
     * 获取当前教师负责的班级列表（用于发布考试时选择）
     */
    @GetMapping("/my-classes")
    public Result<List<TeacherClass>> getMyClasses(@RequestAttribute("userId") Long userId) {
        // 通过userId查询教师信息
        Teacher teacher = teacherMapper.selectByUserId(userId);
        if (teacher == null) {
            return Result.error("未找到教师信息");
        }
        
        List<TeacherClass> classes = teacherClassService.getClassesByTeacherId(teacher.getId());
        
        // 按班级ID去重，保留每个班级的一条记录
        List<TeacherClass> distinctClasses = classes.stream()
            .collect(Collectors.toMap(
                TeacherClass::getClassId, 
                c -> c, 
                (existing, replacement) -> existing
            ))
            .values()
            .stream()
            .collect(Collectors.toList());
        
        return Result.success(distinctClasses);
    }
    
    /**
     * 获取考试监控数据
     */
    @GetMapping("/{id}/monitor")
    public Result<Map<String, Object>> getExamMonitor(@PathVariable Long id) {
        // TODO: 实现考试监控功能
        Map<String, Object> data = new HashMap<>();
        data.put("totalCount", 0);
        data.put("submittedCount", 0);
        data.put("examiningCount", 0);
        data.put("abnormalCount", 0);
        data.put("students", new ArrayList<>());
        return Result.success(data);
    }
    
    /**
     * 延长考试时间
     */
    @PostMapping("/extend-time")
    public Result<Void> extendExamTime(@RequestBody Map<String, Object> params) {
        // TODO: 实现延长考试时间功能
        return Result.success("延长成功", null);
    }
}
