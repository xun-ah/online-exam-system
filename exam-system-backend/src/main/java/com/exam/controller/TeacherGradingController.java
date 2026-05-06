package com.exam.controller;

import com.exam.common.Result;
import com.exam.entity.Exam;
import com.exam.entity.ExamRecord;
import com.exam.entity.Student;
import com.exam.entity.Teacher;
import com.exam.mapper.ExamMapper;
import com.exam.mapper.ExamRecordMapper;
import com.exam.mapper.StudentMapper;
import com.exam.mapper.TeacherMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 教师端阅卷Controller (简化版)
 */
@RestController
@RequestMapping("/teacher/grading")
public class TeacherGradingController {
    
    @Autowired
    private ExamMapper examMapper;
    
    @Autowired
    private ExamRecordMapper examRecordMapper;
    
    @Autowired
    private StudentMapper studentMapper;
    
    @Autowired
    private TeacherMapper teacherMapper;
    
    /**
     * 获取待阅卷列表
     */
    @GetMapping("/pending")
    public Result<List<Map<String, Object>>> getPendingGrading(@RequestAttribute("userId") Long userId) {
        // 获取教师所属院系ID
        Teacher teacher = teacherMapper.selectByUserId(userId);
        if (teacher == null) {
            return Result.error("未找到教师信息");
        }
        
        // 查询本院系的所有考试（通过班级关联院系）
        List<Exam> exams = examMapper.selectListByDepartmentId(teacher.getDepartmentId());
        List<Map<String, Object>> pendingList = new ArrayList<>();
        
        for (Exam exam : exams) {
            List<ExamRecord> records = examRecordMapper.selectList(exam.getId(), null);
            long pendingCount = records.stream()
                .filter(r -> r.getStatus() != null && r.getStatus() == 1)
                .count();
            
            if (pendingCount > 0) {
                Map<String, Object> examData = new HashMap<>();
                examData.put("examId", exam.getId());
                examData.put("examName", exam.getExamName());
                examData.put("className", exam.getPaperName());
                examData.put("pendingCount", pendingCount);
                
                LocalDateTime earliest = records.stream()
                    .filter(r -> r.getStatus() != null && r.getStatus() == 1)
                    .map(ExamRecord::getSubmitTime)
                    .filter(Objects::nonNull)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);
                
                examData.put("submitTime", earliest != null ? earliest.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
                pendingList.add(examData);
            }
        }
        
        return Result.success(pendingList);
    }
    
    /**
     * 获取已阅卷列表
     */
    @GetMapping("/graded")
    public Result<List<Map<String, Object>>> getGradedRecords(
            @RequestParam(required = false) Long examId,
            @RequestAttribute("userId") Long userId) {
        
        // 获取教师所属院系ID
        Teacher teacher = teacherMapper.selectByUserId(userId);
        if (teacher == null) {
            return Result.error("未找到教师信息");
        }
        
        List<ExamRecord> records;
        if (examId != null) {
            // 如果指定了考试ID，直接查询该考试的记录（前端已经确保是本院系的考试）
            records = examRecordMapper.selectList(examId, null);
        } else {
            // 如果没有指定考试ID，查询本院系所有考试的记录
            List<Exam> exams = examMapper.selectListByDepartmentId(teacher.getDepartmentId());
            records = new ArrayList<>();
            for (Exam exam : exams) {
                List<ExamRecord> examRecords = examRecordMapper.selectList(exam.getId(), null);
                records.addAll(examRecords);
            }
        }
        
        List<Map<String, Object>> gradedList = new ArrayList<>();
        
        for (ExamRecord record : records) {
            if (record.getStatus() == null || record.getStatus() != 2) continue;
            
            Student student = studentMapper.selectById(record.getStudentId());
            Map<String, Object> data = new HashMap<>();
            data.put("id", record.getId());
            data.put("studentNo", student != null ? student.getStudentNo() : "");
            data.put("realName", student != null ? student.getRealName() : "");
            data.put("objectiveScore", 0);
            data.put("objectiveTotal", 70);
            data.put("subjectiveScore", 0);
            data.put("subjectiveTotal", 30);
            data.put("totalScore", record.getScore() != null ? record.getScore().intValue() : 0);
            gradedList.add(data);
        }
        
        return Result.success(gradedList);
    }
    
    /**
     * 获取待阅试卷详情
     */
    @GetMapping("/{recordId}")
    public Result<Map<String, Object>> getGradingDetail(@PathVariable Long recordId) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null) return Result.error("考试记录不存在");
        
        Student student = studentMapper.selectById(record.getStudentId());
        Map<String, Object> detail = new HashMap<>();
        detail.put("recordId", record.getId());
        detail.put("studentNo", student != null ? student.getStudentNo() : "");
        detail.put("studentName", student != null ? student.getRealName() : "");
        detail.put("objectiveScore", record.getScore() != null ? record.getScore().intValue() : 0);
        detail.put("subjectiveQuestions", new ArrayList<>());
        
        return Result.success(detail);
    }
    
    /**
     * 提交阅卷评分
     */
    @PostMapping("/submit")
    public Result<Void> submitGrading(@RequestBody Map<String, Object> params, 
                                       @RequestAttribute("userId") Long userId) {
        Long recordId = Long.valueOf(params.get("recordId").toString());
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null) return Result.error("考试记录不存在");
        
        record.setStatus(2);
        examRecordMapper.updateById(record);
        
        return Result.success("评分提交成功", null);
    }
    
    /**
     * 获取成绩统计
     */
    @GetMapping("/statistics/{examId}")
    public Result<Map<String, Object>> getScoreStatistics(@PathVariable Long examId) {
        List<ExamRecord> records = examRecordMapper.selectList(examId, null);
        List<ExamRecord> graded = records.stream()
            .filter(r -> r.getStatus() != null && r.getStatus() == 2)
            .collect(Collectors.toList());
        
        if (graded.isEmpty()) return Result.error("暂无成绩数据");
        
        Map<String, Object> stats = new HashMap<>();
        Optional<BigDecimal> max = graded.stream().map(ExamRecord::getScore).filter(Objects::nonNull).max(BigDecimal::compareTo);
        Optional<BigDecimal> min = graded.stream().map(ExamRecord::getScore).filter(Objects::nonNull).min(BigDecimal::compareTo);
        BigDecimal avg = graded.stream().map(ExamRecord::getScore).filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add).divide(new BigDecimal(graded.size()), 1, BigDecimal.ROUND_HALF_UP);
        
        stats.put("maxScore", max.map(BigDecimal::intValue).orElse(0));
        stats.put("minScore", min.map(BigDecimal::intValue).orElse(0));
        stats.put("avgScore", avg.toString());
        
        long passCount = graded.stream().filter(r -> r.getScore() != null && r.getScore().compareTo(new BigDecimal(60)) >= 0).count();
        stats.put("passRate", String.format("%.1f", (double) passCount / graded.size() * 100));
        
        int total = graded.size();
        stats.put("excellent", (int)(graded.stream().filter(r -> r.getScore() != null && r.getScore().compareTo(new BigDecimal(90)) >= 0).count() * 100 / total));
        stats.put("good", (int)(graded.stream().filter(r -> r.getScore() != null && r.getScore().compareTo(new BigDecimal(80)) >= 0 && r.getScore().compareTo(new BigDecimal(90)) < 0).count() * 100 / total));
        stats.put("medium", (int)(graded.stream().filter(r -> r.getScore() != null && r.getScore().compareTo(new BigDecimal(70)) >= 0 && r.getScore().compareTo(new BigDecimal(80)) < 0).count() * 100 / total));
        stats.put("pass", (int)(graded.stream().filter(r -> r.getScore() != null && r.getScore().compareTo(new BigDecimal(60)) >= 0 && r.getScore().compareTo(new BigDecimal(70)) < 0).count() * 100 / total));
        stats.put("fail", (int)(graded.stream().filter(r -> r.getScore() != null && r.getScore().compareTo(new BigDecimal(60)) < 0).count() * 100 / total));
        
        return Result.success(stats);
    }
    
    /**
     * 导出成绩
     */
    @GetMapping("/export")
    public Result<String> exportScores(@RequestParam(required = false) Long examId) {
        return Result.success("导出功能开发中", null);
    }
}
