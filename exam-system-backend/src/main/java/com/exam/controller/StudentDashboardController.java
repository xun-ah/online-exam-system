package com.exam.controller;

import com.exam.common.Result;
import com.exam.entity.Student;
import com.exam.mapper.StudentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 学生端首页Controller
 */
@RestController
@RequestMapping("/student")
public class StudentDashboardController {

    @Autowired
    private StudentMapper studentMapper;

    /**
     * 获取学生信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getStudentInfo(@RequestAttribute("userId") Long userId) {
        Student student = studentMapper.selectById(userId);
        if (student == null) {
            return Result.error("学生信息不存在");
        }

        Map<String, Object> info = new HashMap<>();
        info.put("id", student.getId());
        info.put("studentNo", student.getStudentNo());
        info.put("realName", student.getRealName());
        info.put("gender", student.getGender());
        info.put("phone", student.getPhone());
        info.put("email", student.getEmail());
        info.put("className", student.getClassName());
        info.put("departmentName", student.getDepartmentName());

        return Result.success(info);
    }

    /**
     * 获取首页统计数据（Mock数据）
     */
    @GetMapping("/dashboard/stats")
    public Result<Map<String, Object>> getDashboardStats(@RequestAttribute("userId") Long userId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pendingCount", 2);
        stats.put("completedCount", 3);
        stats.put("avgScore", 82.5);
        stats.put("classRank", 5);
        return Result.success(stats);
    }

    /**
     * 获取待考考试列表（Mock数据）
     */
    @GetMapping("/exams/pending")
    public Result<List<Map<String, Object>>> getPendingExams(@RequestAttribute("userId") Long userId) {
        List<Map<String, Object>> pendingExams = new ArrayList<>();
        
        Map<String, Object> exam1 = new HashMap<>();
        exam1.put("id", 1);
        exam1.put("examName", "《数据库原理》期中考试");
        exam1.put("startTime", "2026-05-10T14:00:00");
        exam1.put("duration", 90);
        exam1.put("totalScore", 100);
        pendingExams.add(exam1);
        
        Map<String, Object> exam2 = new HashMap<>();
        exam2.put("id", 2);
        exam2.put("examName", "《计算机网络》单元测试");
        exam2.put("startTime", "2026-05-15T10:00:00");
        exam2.put("duration", 60);
        exam2.put("totalScore", 50);
        pendingExams.add(exam2);
        
        return Result.success(pendingExams);
    }

    /**
     * 获取最近考试记录（Mock数据）
     */
    @GetMapping("/exams/recent")
    public Result<List<Map<String, Object>>> getRecentExams(@RequestAttribute("userId") Long userId) {
        List<Map<String, Object>> recentExams = new ArrayList<>();
        
        Map<String, Object> exam1 = new HashMap<>();
        exam1.put("id", 1);
        exam1.put("examName", "《Java程序设计》期末");
        exam1.put("score", 88);
        recentExams.add(exam1);
        
        Map<String, Object> exam2 = new HashMap<>();
        exam2.put("id", 2);
        exam2.put("examName", "《数据结构》期中");
        exam2.put("score", 92);
        recentExams.add(exam2);
        
        Map<String, Object> exam3 = new HashMap<>();
        exam3.put("id", 3);
        exam3.put("examName", "《英语四级》模拟");
        exam3.put("score", 76);
        recentExams.add(exam3);
        
        return Result.success(recentExams);
    }

    /**
     * 获取成绩趋势（Mock数据）
     */
    @GetMapping("/score/trend")
    public Result<List<Map<String, Object>>> getScoreTrend(@RequestAttribute("userId") Long userId) {
        List<Map<String, Object>> trend = new ArrayList<>();
        
        String[] examNames = {"考试1", "考试2", "考试3", "考试4", "考试5"};
        int[] scores = {85, 78, 92, 88, 75};
        
        for (int i = 0; i < examNames.length; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("examName", examNames[i]);
            item.put("score", scores[i]);
            trend.add(item);
        }
        
        return Result.success(trend);
    }
}
