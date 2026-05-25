package com.exam.controller;

import com.exam.annotation.SysLog;
import com.exam.common.Result;
import com.exam.entity.Exam;
import com.exam.entity.ExamRecord;
import com.exam.entity.Paper;
import com.exam.entity.Question;
import com.exam.entity.Teacher;
import com.exam.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/teacher/dashboard")
public class TeacherDashboardController {

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private PaperMapper paperMapper;

    @Autowired
    private QuestionMapper questionMapper;
    
    @Autowired
    private ExamRecordMapper examRecordMapper;
    
    @Autowired
    private TeacherClassMapper teacherClassMapper;

    /**
     * 获取仪表盘统计数据
     */
    @SysLog("获取教师仪表盘统计")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(@RequestAttribute("userId") Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 获取教师信息（通过userId查询）
        Teacher teacher = teacherMapper.selectByUserId(userId);
        if (teacher != null) {
            stats.put("teacherName", teacher.getRealName());
            stats.put("teacherNo", teacher.getTeacherNo());
            stats.put("departmentName", teacher.getDepartmentName());
            
            Long teacherId = teacher.getId();
            
            // 题库统计 - 使用前端期望的字段名
            int totalQuestions = questionMapper.countByTeacherId(teacherId);
            stats.put("questionCount", totalQuestions);
            stats.put("totalQuestions", totalQuestions);
            stats.put("monthlyNewQuestions", 0); // 可后续增加按月统计
            stats.put("monthQuestions", 0);

            // 试卷统计
            int totalPapers = paperMapper.countByTeacherId(teacherId);
            stats.put("paperCount", totalPapers);
            stats.put("totalPapers", totalPapers);
            stats.put("monthlyNewPapers", 0);
            stats.put("monthPapers", 0);

            // 考试统计
            int totalExams = examMapper.countByTeacherId(teacherId);
            int ongoingExams = examMapper.countOngoingByTeacherId(teacherId);
            stats.put("examCount", totalExams);
            stats.put("totalExams", totalExams);
            stats.put("ongoingExamCount", ongoingExams);
            stats.put("ongoingExams", ongoingExams);

            // 待阅卷统计 - 计算实际需要阅卷的数量
            int pendingGrading = 0;
            List<Exam> exams = examMapper.selectList(teacherId, null, null);
            for (Exam exam : exams) {
                List<ExamRecord> records = examRecordMapper.selectList(exam.getId(), null);
                long count = records.stream()
                    .filter(r -> r.getStatus() != null && r.getStatus() == 1)
                    .count();
                pendingGrading += count;
            }
            stats.put("pendingGradingCount", pendingGrading);
            stats.put("pendingGrading", pendingGrading);
            stats.put("urgentGradingCount", pendingGrading > 20 ? pendingGrading : 0);
        } else {
            stats.put("teacherName", "未知教师");
            stats.put("teacherNo", "");
            stats.put("departmentName", "");
            stats.put("questionCount", 0);
            stats.put("totalQuestions", 0);
            stats.put("monthlyNewQuestions", 0);
            stats.put("paperCount", 0);
            stats.put("totalPapers", 0);
            stats.put("monthlyNewPapers", 0);
            stats.put("examCount", 0);
            stats.put("totalExams", 0);
            stats.put("ongoingExamCount", 0);
            stats.put("pendingGradingCount", 0);
            stats.put("urgentGradingCount", 0);
        }

        return Result.success(stats);
    }
    
    /**
     * 获取待办事项列表
     */
    @SysLog("获取教师待办事项")
    @GetMapping("/todos")
    public Result<List<Map<String, Object>>> getTodos(@RequestAttribute("userId") Long userId) {
        List<Map<String, Object>> todos = new ArrayList<>();
        
        // 获取教师信息（通过userId查询）
        Teacher teacher = teacherMapper.selectByUserId(userId);
        if (teacher == null) {
            return Result.success(todos);
        }
        
        Long teacherId = teacher.getId();
        
        // 1. 待阅卷任务
        List<Exam> exams = examMapper.selectList(teacherId, null, null);
        for (Exam exam : exams) {
            List<ExamRecord> records = examRecordMapper.selectList(exam.getId(), null);
            long pendingCount = records.stream()
                .filter(r -> r.getStatus() != null && r.getStatus() == 1)
                .count();
            
            if (pendingCount > 0) {
                Map<String, Object> todo = new HashMap<>();
                todo.put("id", exam.getId() + "_grading");
                todo.put("title", "《" + exam.getExamName() + "》阅卷");
                todo.put("description", "待阅卷 " + pendingCount + " 份");
                todo.put("type", "grading");
                todo.put("priority", pendingCount > 20 ? "urgent" : "normal");
                todo.put("createTime", exam.getCreateTime());
                todos.add(todo);
            }
        }
        
        // 2. 进行中的考试监控
        for (Exam exam : exams) {
            if (exam.getStatus() != null && exam.getStatus() == 1) {
                Map<String, Object> todo = new HashMap<>();
                todo.put("id", exam.getId() + "_monitor");
                todo.put("title", "《" + exam.getExamName() + "》考试监控");
                
                // 计算剩余时间
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime endTime = exam.getEndTime();
                if (endTime != null && endTime.isAfter(now)) {
                    long minutes = java.time.Duration.between(now, endTime).toMinutes();
                    todo.put("description", "进行中，剩余 " + minutes + " 分钟");
                } else {
                    todo.put("description", "进行中");
                }
                
                todo.put("type", "monitoring");
                todo.put("priority", "warning");
                todo.put("createTime", exam.getStartTime());
                todos.add(todo);
            }
        }
        
        // 3. 待审核试卷
        List<Paper> papers = paperMapper.selectList(teacherId);
        long pendingPaperCount = papers.stream()
            .filter(p -> p.getStatus() != null && p.getStatus().equals("unpublished"))
            .count();
        
        if (pendingPaperCount > 0) {
            Map<String, Object> todo = new HashMap<>();
            todo.put("id", "paper_review");
            todo.put("title", "试卷审核");
            todo.put("description", "需审核 " + pendingPaperCount + " 套试卷");
            todo.put("type", "review");
            todo.put("priority", "normal");
            todo.put("createTime", LocalDateTime.now());
            todos.add(todo);
        }
        
        // 按创建时间排序
        todos.sort((a, b) -> {
            LocalDateTime timeA = (LocalDateTime) a.get("createTime");
            LocalDateTime timeB = (LocalDateTime) b.get("createTime");
            if (timeA == null && timeB == null) return 0;
            if (timeA == null) return 1;
            if (timeB == null) return -1;
            return timeB.compareTo(timeA);
        });
        
        // 只返回前5个
        return Result.success(todos.stream().limit(5).collect(Collectors.toList()));
    }
    
    /**
     * 获取近期考试列表
     */
    @SysLog("获取近期考试")
    @GetMapping("/recent-exams")
    public Result<List<Map<String, Object>>> getRecentExams(@RequestAttribute("userId") Long userId) {
        List<Map<String, Object>> examList = new ArrayList<>();
        
        // 获取教师信息（通过userId查询）
        Teacher teacher = teacherMapper.selectByUserId(userId);
        if (teacher == null) {
            return Result.success(examList);
        }
        
        Long teacherId = teacher.getId();
        
        // 获取该教师的所有考试
        List<Exam> exams = examMapper.selectList(teacherId, null, null);
        
        // 按开始时间降序排序，取前5个
        exams.sort((a, b) -> {
            if (a.getStartTime() == null && b.getStartTime() == null) return 0;
            if (a.getStartTime() == null) return 1;
            if (b.getStartTime() == null) return -1;
            return b.getStartTime().compareTo(a.getStartTime());
        });
        
        for (Exam exam : exams.stream().limit(5).collect(Collectors.toList())) {
            Map<String, Object> examData = new HashMap<>();
            examData.put("id", exam.getId());
            examData.put("examName", exam.getExamName());
            examData.put("startTime", exam.getStartTime());
            examData.put("status", exam.getStatus());
            
            // 统计参考人数
            List<ExamRecord> records = examRecordMapper.selectList(exam.getId(), null);
            examData.put("participantCount", records.size());
            
            examList.add(examData);
        }
        
        return Result.success(examList);
    }
    
    /**
     * 获取教学数据概览
     */
    @SysLog("获取教学数据概览")
    @GetMapping("/teaching-data")
    public Result<Map<String, Object>> getTeachingData(@RequestAttribute("userId") Long userId) {
        Map<String, Object> data = new HashMap<>();
        
        try {
            // 获取教师信息（通过userId查询）
            Teacher teacher = teacherMapper.selectByUserId(userId);
            if (teacher == null) {
                return Result.success(data);
            }
            
            Long teacherId = teacher.getId();
            
            // 1. 各科目平均分
            List<Exam> exams = examMapper.selectList(teacherId, null, null);
            Map<String, List<Double>> subjectScores = new HashMap<>();
            
            for (Exam exam : exams) {
                List<ExamRecord> records = examRecordMapper.selectList(exam.getId(), null);
                String subject = exam.getExamName();
                
                // 简化处理：从考试名称中提取科目名称（去除《》和后面的描述）
                if (subject != null && subject.contains("》")) {
                    subject = subject.substring(1, subject.indexOf("》"));
                }
                
                for (ExamRecord record : records) {
                    if (record.getScore() != null && record.getStatus() != null && record.getStatus() >= 2) {
                        subjectScores.computeIfAbsent(subject, k -> new ArrayList<>()).add(record.getScore().doubleValue());
                    }
                }
            }
            
            List<Map<String, Object>> subjectData = new ArrayList<>();
            String[] colors = {"#409eff", "#67c23a", "#e6a23c", "#7c3aed", "#2dd4bf"};
            int colorIndex = 0;
            
            for (Map.Entry<String, List<Double>> entry : subjectScores.entrySet()) {
                List<Double> scores = entry.getValue();
                double avgScore = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                
                Map<String, Object> subject = new HashMap<>();
                subject.put("name", entry.getKey());
                subject.put("score", Math.round(avgScore * 10) / 10.0);
                subject.put("percent", avgScore); // 用于柱状图高度
                subject.put("color", colors[colorIndex % colors.length]);
                subjectData.add(subject);
                colorIndex++;
            }
            
            data.put("subjectData", subjectData);
            
            // 2. 题型使用分布
            List<Question> questions = questionMapper.selectList(teacherId, null, null, null, null, 0, 10000);
            Map<Integer, Long> questionTypeCount = questions.stream()
                .filter(q -> q.getType() != null)
                .collect(Collectors.groupingBy(Question::getType, Collectors.counting()));
            
            long totalQuestions = questions.size();
            List<Map<String, Object>> questionTypeData = new ArrayList<>();
            String[] typeNames = {"单选", "多选", "判断", "填空", "简答"};
            String[] typeColors = {"#409eff", "#67c23a", "#e6a23c", "#f56c6c", "#7c3aed"};
            
            int accumulatedPercent = 0;
            for (int i = 0; i < typeNames.length && i < 5; i++) {
                long count = questionTypeCount.getOrDefault(i + 1, 0L);
                int percent = totalQuestions > 0 ? (int) (count * 100 / totalQuestions) : 0;
                
                Map<String, Object> typeData = new HashMap<>();
                typeData.put("name", typeNames[i]);
                typeData.put("percent", percent);
                typeData.put("color", typeColors[i]);
                typeData.put("rotate", accumulatedPercent * 3.6); // 转换为角度
                questionTypeData.add(typeData);
                
                accumulatedPercent += percent;
            }
            
            data.put("questionTypeData", questionTypeData);
            
            // 3. 教学统计信息
            data.put("courseCount", teacherMapper.countSubjectsByTeacherId(teacherId));
            data.put("studentCount", teacherMapper.countStudentsByTeacherId(teacherId));
            data.put("examSessionCount", exams.size());
            
            // 计算及格率
            int totalRecords = 0;
            int passedRecords = 0;
            for (Exam exam : exams) {
                List<ExamRecord> records = examRecordMapper.selectList(exam.getId(), null);
                for (ExamRecord record : records) {
                    if (record.getStatus() != null && record.getStatus() >= 2) {
                        totalRecords++;
                        if (record.getScore() != null && record.getScore().doubleValue() >= 60) {
                            passedRecords++;
                        }
                    }
                }
            }
            double passRate = totalRecords > 0 ? (passedRecords * 100.0 / totalRecords) : 0;
            data.put("passRate", Math.round(passRate * 10) / 10.0);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取教学数据失败: " + e.getMessage());
        }
        
        return Result.success(data);
    }
}
