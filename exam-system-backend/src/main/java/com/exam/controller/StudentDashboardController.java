package com.exam.controller;

import com.exam.common.Result;
import com.exam.entity.Exam;
import com.exam.entity.ExamRecord;
import com.exam.entity.Paper;
import com.exam.entity.Question;
import com.exam.entity.Student;
import com.exam.mapper.ExamMapper;
import com.exam.mapper.ExamRecordMapper;
import com.exam.mapper.PaperMapper;
import com.exam.mapper.QuestionMapper;
import com.exam.mapper.StudentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学生端首页Controller
 */
@RestController
@RequestMapping("/student")
public class StudentDashboardController {

    @Autowired
    private StudentMapper studentMapper;
    
    @Autowired
    private ExamMapper examMapper;
    
    @Autowired
    private ExamRecordMapper examRecordMapper;
    
    @Autowired
    private PaperMapper paperMapper;
    
    @Autowired
    private QuestionMapper questionMapper;

    /**
     * 获取学生信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getStudentInfo(@RequestAttribute("userId") Long userId) {
        // 通过userId查询学生信息（student表的user_id字段）
        Student student = studentMapper.selectByUserId(userId);
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
     * 获取首页统计数据
     */
    @GetMapping("/dashboard/stats")
    public Result<Map<String, Object>> getDashboardStats(@RequestAttribute("userId") Long userId) {
        // 获取学生信息
        Student student = studentMapper.selectByUserId(userId);
        if (student == null) {
            return Result.error("学生信息不存在");
        }
        
        Map<String, Object> stats = new HashMap<>();
        
        // 查询学生所在班级的所有考试
        List<Exam> exams = examMapper.selectListByClassId(student.getClassId());
        if (exams == null || exams.isEmpty()) {
            stats.put("pendingCount", 0);
            stats.put("completedCount", 0);
            stats.put("avgScore", 0);
            stats.put("classRank", 0);
            return Result.success(stats);
        }
        
        LocalDateTime now = LocalDateTime.now();
        int pendingCount = 0;
        int completedCount = 0;
        
        // 统计待考和已完成的考试数量
        for (Exam exam : exams) {
            if (exam.getDeleted() != null && exam.getDeleted() == 1) {
                continue;
            }
            
            if (now.isAfter(exam.getEndTime())) {
                // 已结束
                completedCount++;
            } else {
                // 未开始或进行中，都算待考
                pendingCount++;
            }
        }
        
        stats.put("pendingCount", pendingCount);
        stats.put("completedCount", completedCount);
        
        // 计算真实平均分和班级排名
        List<ExamRecord> studentRecords = examRecordMapper.selectList(null, student.getId());
        if (studentRecords != null && !studentRecords.isEmpty()) {
            List<Double> scores = studentRecords.stream()
                .filter(r -> r.getStatus() != null && r.getStatus() == 2 && r.getScore() != null)
                .map(r -> r.getScore().doubleValue())
                .collect(Collectors.toList());
            
            if (!scores.isEmpty()) {
                double avgScore = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                stats.put("avgScore", Math.round(avgScore * 10) / 10.0);
            } else {
                stats.put("avgScore", 0);
            }
            
            // 计算班级排名：获取同班级所有学生的平均分进行排名
            List<Student> classStudents = studentMapper.selectListByClassId(student.getClassId());
            if (classStudents != null && !classStudents.isEmpty()) {
                List<Map<String, Object>> studentAvgScores = new ArrayList<>();
                for (Student s : classStudents) {
                    List<ExamRecord> sRecords = examRecordMapper.selectList(null, s.getId());
                    if (sRecords != null && !sRecords.isEmpty()) {
                        List<Double> sScores = sRecords.stream()
                            .filter(r -> r.getStatus() != null && r.getStatus() == 2 && r.getScore() != null)
                            .map(r -> r.getScore().doubleValue())
                            .collect(Collectors.toList());
                        if (!sScores.isEmpty()) {
                            double sAvg = sScores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                            Map<String, Object> sData = new HashMap<>();
                            sData.put("studentId", s.getId());
                            sData.put("avgScore", sAvg);
                            studentAvgScores.add(sData);
                        }
                    }
                }
                
                // 按平均分降序排序
                studentAvgScores.sort((a, b) -> Double.compare((Double) b.get("avgScore"), (Double) a.get("avgScore")));
                
                // 找到当前学生的排名
                int rank = 1;
                for (Map<String, Object> sData : studentAvgScores) {
                    if (sData.get("studentId").equals(student.getId())) {
                        break;
                    }
                    rank++;
                }
                stats.put("classRank", rank);
            } else {
                stats.put("classRank", 0);
            }
        } else {
            stats.put("avgScore", 0);
            stats.put("classRank", 0);
        }
        
        return Result.success(stats);
    }

    /**
     * 获取待考考试列表（支持分页）
     */
    @GetMapping("/exams/pending")
    public Result<Map<String, Object>> getPendingExams(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestAttribute("userId") Long userId) {
        // 获取学生信息
        Student student = studentMapper.selectByUserId(userId);
        if (student == null) {
            return Result.error("学生信息不存在");
        }
        
        // 查询该学生已参加的考试记录
        List<ExamRecord> studentRecords = examRecordMapper.selectList(null, student.getId());
        Set<Long> completedExamIds = new HashSet<>();
        if (studentRecords != null) {
            for (ExamRecord record : studentRecords) {
                // status=1(已提交)或status=2(已阅卷)表示已参加过
                if (record.getStatus() != null && (record.getStatus() == 1 || record.getStatus() == 2)) {
                    completedExamIds.add(record.getExamId());
                }
            }
        }
        
        // 查询该学生正在进行的考试记录(status=0)
        Set<Long> ongoingExamIds = new HashSet<>();
        if (studentRecords != null) {
            for (ExamRecord record : studentRecords) {
                if (record.getStatus() != null && record.getStatus() == 0) {
                    ongoingExamIds.add(record.getExamId());
                }
            }
        }
        
        // 根据班级ID查询考试列表（只显示该班级的考试）
        List<Exam> exams = examMapper.selectListByClassId(student.getClassId());
        if (exams == null || exams.isEmpty()) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("records", new ArrayList<>());
            emptyResult.put("total", 0);
            emptyResult.put("pageNum", pageNum);
            emptyResult.put("pageSize", pageSize);
            return Result.success(emptyResult);
        }
        
        // 过滤未删除的考试，并计算动态状态
        List<Map<String, Object>> pendingExams = exams.stream()
            .filter(exam -> exam.getDeleted() == null || exam.getDeleted() == 0)
            .map(exam -> {
                // 如果学生已经参加过该考试，标记为已完成
                if (completedExamIds.contains(exam.getId())) {
                    // 计算考试时长
                    Integer duration = exam.getDuration();
                    if (duration == null && exam.getStartTime() != null && exam.getEndTime() != null) {
                        duration = (int) java.time.Duration.between(exam.getStartTime(), exam.getEndTime()).toMinutes();
                    }
                    
                    // 获取试卷总分
                    Integer totalScore = 0;
                    if (exam.getPaperId() != null) {
                        Paper paper = paperMapper.selectById(exam.getPaperId());
                        if (paper != null) {
                            if (paper.getTotalScore() != null) {
                                totalScore = paper.getTotalScore().intValue();
                            } else if (paper.getQuestionConfig() != null) {
                                try {
                                    cn.hutool.json.JSONObject config = cn.hutool.json.JSONUtil.parseObj(paper.getQuestionConfig());
                                    cn.hutool.json.JSONArray questions = config.getJSONArray("questions");
                                    int calculatedScore = 0;
                                    if (questions != null) {
                                        for (int i = 0; i < questions.size(); i++) {
                                            cn.hutool.json.JSONObject q = questions.getJSONObject(i);
                                            Object scoreObj = q.get("score");
                                            if (scoreObj instanceof Number) {
                                                calculatedScore += ((Number) scoreObj).intValue();
                                            }
                                        }
                                    }
                                    totalScore = calculatedScore;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    totalScore = 0;
                                }
                            }
                        }
                    }
                    
                    Map<String, Object> examData = new HashMap<>();
                    examData.put("id", exam.getId());
                    examData.put("examName", exam.getExamName());
                    examData.put("paperName", exam.getPaperName());
                    examData.put("startTime", exam.getStartTime());
                    examData.put("endTime", exam.getEndTime());
                    examData.put("duration", duration);
                    examData.put("totalScore", totalScore);
                    examData.put("className", exam.getClassName());
                    examData.put("status", 2); // 已完成
                    examData.put("completed", true);
                    examData.put("absentReason", null); // 已参加，无缺考原因
                    return examData;
                }
                
                // 计算考试状态
                LocalDateTime now = LocalDateTime.now();
                int status;
                String absentReason = null;
                
                if (now.isBefore(exam.getStartTime())) {
                    status = 0; // 未开始
                    // 检查是否已开始创建考试记录但未进入
                    if (ongoingExamIds.contains(exam.getId())) {
                        absentReason = "考试已开始，请尽快进入考场";
                    }
                } else if (!now.isBefore(exam.getStartTime()) && !now.isAfter(exam.getEndTime())) {
                    status = 1; // 进行中（包含边界时间）
                    // 检查是否已开始但未进入
                    if (ongoingExamIds.contains(exam.getId())) {
                        absentReason = "考试进行中，请尽快进入考场";
                    } else {
                        absentReason = "考试进行中，您尚未参加";
                    }
                } else {
                    status = 2; // 已结束
                    absentReason = "考试已结束，您未参加（缺考）";
                }
                
                // 确保 duration 不为 null，如果为 null 则根据 startTime 和 endTime 计算
                Integer duration = exam.getDuration();
                if (duration == null && exam.getStartTime() != null && exam.getEndTime() != null) {
                    duration = (int) java.time.Duration.between(exam.getStartTime(), exam.getEndTime()).toMinutes();
                }
                
                // 获取试卷总分
                Integer totalScore = 0;
                if (exam.getPaperId() != null) {
                    Paper paper = paperMapper.selectById(exam.getPaperId());
                    if (paper != null) {
                        if (paper.getTotalScore() != null) {
                            totalScore = paper.getTotalScore().intValue();
                            System.out.println("[总分计算] 从数据库读取: " + totalScore);
                        } else if (paper.getQuestionConfig() != null) {
                            // 如果总分为空，动态计算
                            try {
                                cn.hutool.json.JSONObject config = cn.hutool.json.JSONUtil.parseObj(paper.getQuestionConfig());
                                cn.hutool.json.JSONArray questions = config.getJSONArray("questions");
                                int calculatedScore = 0;
                                if (questions != null) {
                                    for (int i = 0; i < questions.size(); i++) {
                                        cn.hutool.json.JSONObject q = questions.getJSONObject(i);
                                        Object scoreObj = q.get("score");
                                        if (scoreObj instanceof Number) {
                                            calculatedScore += ((Number) scoreObj).intValue();
                                        }
                                    }
                                }
                                totalScore = calculatedScore;
                                System.out.println("[总分计算] 动态计算: " + totalScore + ", paperId=" + paper.getId() + ", config=" + paper.getQuestionConfig());
                            } catch (Exception e) {
                                e.printStackTrace();
                                totalScore = 0;
                            }
                        }
                    }
                }
                
                Map<String, Object> examData = new HashMap<>();
                examData.put("id", exam.getId());
                examData.put("examName", exam.getExamName());
                examData.put("paperName", exam.getPaperName());
                examData.put("startTime", exam.getStartTime());
                examData.put("endTime", exam.getEndTime());
                examData.put("duration", duration);
                examData.put("totalScore", totalScore);
                examData.put("className", exam.getClassName());
                examData.put("status", status);
                examData.put("completed", false);
                examData.put("absentReason", absentReason); // 未考原因
                
                return examData;
            })
            .sorted((a, b) -> {
                // 按开始时间倒序，最近的考试在前
                LocalDateTime startTimeA = (LocalDateTime) a.get("startTime");
                LocalDateTime startTimeB = (LocalDateTime) b.get("startTime");
                System.out.println("[排序] 考试A: " + a.get("examName") + " 时间: " + startTimeA + ", 考试B: " + b.get("examName") + " 时间: " + startTimeB);
                return startTimeB.compareTo(startTimeA); // 倒序
            })
            .collect(Collectors.toList());
        
        // 实现分页
        int total = pendingExams.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        
        List<Map<String, Object>> pageList;
        if (fromIndex < total) {
            pageList = pendingExams.subList(fromIndex, toIndex);
        } else {
            pageList = new ArrayList<>();
        }
        
        // 返回分页结果
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageList);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        
        return Result.success(result);
    }

    /**
     * 获取历史考试记录列表
     */
    @GetMapping("/exams/records")
    public Result<List<Map<String, Object>>> getExamRecords(@RequestAttribute("userId") Long userId) {
        // 获取学生信息
        Student student = studentMapper.selectByUserId(userId);
        if (student == null) {
            return Result.error("学生信息不存在");
        }
        
        // 查询学生的考试记录
        List<ExamRecord> records = examRecordMapper.selectList(null, student.getId());
        if (records == null || records.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        
        // 只返回已提交或已阅卷的记录（status >= 1）
        records = records.stream()
            .filter(record -> record.getStatus() != null && record.getStatus() >= 1)
            .collect(Collectors.toList());
        
        if (records.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        
        // 获取所有考试信息
        List<Exam> exams = examMapper.selectListByDepartmentId(student.getDepartmentId());
        Map<Long, Exam> examMap = new HashMap<>();
        if (exams != null) {
            for (Exam exam : exams) {
                examMap.put(exam.getId(), exam);
            }
        }
        
        // 组装考试记录数据
        List<Map<String, Object>> recordList = records.stream()
            .map(record -> {
                Exam exam = examMap.get(record.getExamId());
                if (exam == null) {
                    return null;
                }
                
                Map<String, Object> data = new HashMap<>();
                data.put("id", record.getId());
                data.put("examId", exam.getId());
                data.put("examName", exam.getExamName());
                data.put("paperName", exam.getPaperName());
                data.put("className", exam.getClassName());
                data.put("startTime", exam.getStartTime());
                data.put("endTime", exam.getEndTime());
                
                // 计算考试时长
                Integer duration = exam.getDuration();
                if (duration == null && exam.getStartTime() != null && exam.getEndTime() != null) {
                    duration = (int) java.time.Duration.between(exam.getStartTime(), exam.getEndTime()).toMinutes();
                }
                data.put("duration", duration);
                
                data.put("score", record.getScore());
                data.put("submitTime", record.getSubmitTime());
                data.put("status", record.getStatus());
                
                return data;
            })
            .filter(Objects::nonNull)
            .sorted((a, b) -> {
                LocalDateTime timeA = (LocalDateTime) a.get("startTime");
                LocalDateTime timeB = (LocalDateTime) b.get("startTime");
                return timeB.compareTo(timeA); // 按开始时间降序
            })
            .collect(Collectors.toList());
        
        return Result.success(recordList);
    }

    /**
     * 获取最近考试记录
     */
    @GetMapping("/exams/recent")
    public Result<Map<String, Object>> getRecentExams(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "2") int pageSize,
            @RequestAttribute("userId") Long userId) {
        // 获取学生信息
        Student student = studentMapper.selectByUserId(userId);
        if (student == null) {
            return Result.error("学生信息不存在");
        }
        
        // 查询学生的考试记录
        List<ExamRecord> records = examRecordMapper.selectList(null, student.getId());
        if (records == null || records.isEmpty()) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("records", new ArrayList<>());
            emptyResult.put("total", 0);
            emptyResult.put("pageNum", pageNum);
            emptyResult.put("pageSize", pageSize);
            return Result.success(emptyResult);
        }
        
        // 获取考试信息
        List<Exam> exams = examMapper.selectListByDepartmentId(student.getDepartmentId());
        Map<Long, Exam> examMap = new HashMap<>();
        if (exams != null) {
            for (Exam exam : exams) {
                examMap.put(exam.getId(), exam);
            }
        }
        
        // 组装数据，获取所有已提交的记录
        List<Map<String, Object>> allRecentList = records.stream()
            .filter(record -> record.getStatus() != null && record.getStatus() == 2) // 已提交
            .map(record -> {
                Exam exam = examMap.get(record.getExamId());
                if (exam == null) {
                    return null;
                }
                
                Map<String, Object> data = new HashMap<>();
                data.put("id", record.getId());
                data.put("examName", exam.getExamName());
                data.put("score", record.getScore());
                
                return data;
            })
            .filter(Objects::nonNull)
            .sorted((a, b) -> Long.compare((Long) b.get("id"), (Long) a.get("id"))) // 按记录ID降序
            .collect(Collectors.toList());
        
        // 分页处理
        int total = allRecentList.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        
        List<Map<String, Object>> pagedList;
        if (fromIndex >= total) {
            pagedList = new ArrayList<>();
        } else {
            pagedList = allRecentList.subList(fromIndex, toIndex);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("records", pagedList);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        
        return Result.success(result);
    }

    /**
     * 获取成绩趋势
     */
    @GetMapping("/score/trend")
    public Result<List<Map<String, Object>>> getScoreTrend(@RequestAttribute("userId") Long userId) {
        try {
            Student student = studentMapper.selectByUserId(userId);
            if (student == null) {
                return Result.success(new ArrayList<>());
            }
            
            List<ExamRecord> records = examRecordMapper.selectList(null, student.getId());
            if (records == null || records.isEmpty()) {
                return Result.success(new ArrayList<>());
            }
            
            List<Map<String, Object>> trendList = new ArrayList<>();
            for (ExamRecord record : records) {
                if (record.getStatus() == null || record.getStatus() != 2) continue;
                
                Exam exam = examMapper.selectById(record.getExamId());
                if (exam == null) continue;
                
                Map<String, Object> data = new HashMap<>();
                data.put("id", record.getId());
                data.put("examName", exam.getExamName());
                data.put("score", record.getScore() != null ? record.getScore().doubleValue() : 0);
                trendList.add(data);
            }
            
            // 按记录ID升序（最早的在前，按时间顺序显示趋势），限制10条
            trendList.sort((a, b) -> Long.compare((Long) a.get("id"), (Long) b.get("id")));
            return Result.success(trendList.stream().limit(10).collect(Collectors.toList()));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.success(new ArrayList<>());
        }
    }
    
    /**
     * 获取个人成绩列表
     */
    @GetMapping("/scores")
    public Result<List<Map<String, Object>>> getScoreList(@RequestAttribute("userId") Long userId) {
        Student student = studentMapper.selectByUserId(userId);
        if (student == null) {
            return Result.error("学生信息不存在");
        }
        
        List<ExamRecord> records = examRecordMapper.selectList(null, student.getId());
        if (records == null || records.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        
        List<Exam> exams = examMapper.selectListByDepartmentId(student.getDepartmentId());
        Map<Long, Exam> examMap = new HashMap<>();
        if (exams != null) {
            for (Exam exam : exams) {
                examMap.put(exam.getId(), exam);
            }
        }
        
        List<Map<String, Object>> scoreList = records.stream()
            .filter(record -> record.getStatus() != null && record.getStatus() == 2)
            .map(record -> {
                Exam exam = examMap.get(record.getExamId());
                if (exam == null) return null;
                
                Map<String, Object> data = new HashMap<>();
                data.put("id", record.getId());
                data.put("examId", record.getExamId());
                data.put("examName", exam.getExamName());
                data.put("score", record.getScore());
                data.put("submitTime", record.getSubmitTime());
                // 计算用时（从创建到提交的时间差，单位：分钟）
                if (record.getCreateTime() != null && record.getSubmitTime() != null) {
                    long minutes = java.time.Duration.between(record.getCreateTime(), record.getSubmitTime()).toMinutes();
                    data.put("duration", minutes);
                } else {
                    data.put("duration", 0);
                }
                
                return data;
            })
            .filter(Objects::nonNull)
            .sorted((a, b) -> Long.compare((Long) b.get("id"), (Long) a.get("id")))
            .collect(Collectors.toList());
        
        return Result.success(scoreList);
    }
    
    /**
     * 获取知识点掌握雷达图数据
     */
    @GetMapping("/scores/knowledge-radar")
    public Result<List<Map<String, Object>>> getKnowledgeRadar(@RequestAttribute("userId") Long userId) {
        Student student = studentMapper.selectByUserId(userId);
        if (student == null) {
            return Result.error("学生信息不存在");
        }
        
        // 查询学生的所有已阅卷考试记录
        List<ExamRecord> records = examRecordMapper.selectList(null, student.getId());
        if (records == null || records.isEmpty()) {
            // 没有考试记录，返回空列表
            return Result.success(new ArrayList<>());
        }
        
        // 按科目分组计算平均分
        Map<String, List<Double>> subjectScores = new HashMap<>();
        
        for (ExamRecord record : records) {
            // 只统计已阅卷的记录 (status=2)
            if (record.getStatus() == null || record.getStatus() != 2 || record.getScore() == null) {
                continue;
            }
            
            // 获取考试信息
            Exam exam = examMapper.selectById(record.getExamId());
            if (exam == null || exam.getPaperId() == null) {
                continue;
            }
            
            // 获取试卷信息，获取科目
            Paper paper = paperMapper.selectById(exam.getPaperId());
            if (paper == null || paper.getSubject() == null || paper.getSubject().trim().isEmpty()) {
                continue;
            }
            
            String subjectName = paper.getSubject();
            subjectScores.computeIfAbsent(subjectName, k -> new ArrayList<>())
                .add(record.getScore().doubleValue());
        }
        
        // 计算每个科目的平均分，生成雷达图数据
        List<Map<String, Object>> radarData = new ArrayList<>();
        for (Map.Entry<String, List<Double>> entry : subjectScores.entrySet()) {
            List<Double> scores = entry.getValue();
            double avgScore = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            
            Map<String, Object> item = new HashMap<>();
            item.put("name", entry.getKey());
            item.put("score", Math.round(avgScore * 10) / 10.0); // 保留1位小数
            radarData.add(item);
        }
        
        return Result.success(radarData);
    }
    
    /**
     * 获取错题本列表
     */
    @GetMapping("/wrong-book")
    public Result<List<Map<String, Object>>> getWrongBook(@RequestAttribute("userId") Long userId) {
        Student student = studentMapper.selectByUserId(userId);
        if (student == null) {
            return Result.error("学生信息不存在");
        }
        
        // 查询学生的所有已阅卷考试记录
        List<ExamRecord> records = examRecordMapper.selectList(null, student.getId());
        if (records == null || records.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        
        // 获取考试和试卷信息
        List<Exam> exams = examMapper.selectListByDepartmentId(student.getDepartmentId());
        Map<Long, Exam> examMap = new HashMap<>();
        if (exams != null) {
            for (Exam exam : exams) {
                examMap.put(exam.getId(), exam);
            }
        }
        
        // 收集所有错题
        List<Map<String, Object>> wrongQuestions = new ArrayList<>();
        
        for (ExamRecord record : records) {
            if (record.getStatus() != null && record.getStatus() == 2 && record.getAnswers() != null) {
                Exam exam = examMap.get(record.getExamId());
                if (exam == null || exam.getPaperId() == null) continue;
                
                Paper paper = paperMapper.selectById(exam.getPaperId());
                if (paper == null || paper.getQuestionConfig() == null) continue;
                
                // 解析试卷题目配置
                cn.hutool.json.JSONObject config = cn.hutool.json.JSONUtil.parseObj(paper.getQuestionConfig());
                List<cn.hutool.json.JSONObject> questions = config.getJSONArray("questions").toList(cn.hutool.json.JSONObject.class);
                
                // 解析学生答案（兼容数组和对象两种格式）
                String answersStr = record.getAnswers();
                if (answersStr == null || answersStr.trim().isEmpty()) {
                    continue;
                }
                
                // 构建答案映射：questionId -> answer
                java.util.Map<Long, String> answerMap = new java.util.HashMap<>();
                
                try {
                    if (answersStr.trim().startsWith("[")) {
                        // 数组格式：[{"questionId":180,"answer":"B"}...]
                        cn.hutool.json.JSONArray answerArray = cn.hutool.json.JSONUtil.parseArray(answersStr);
                        for (int i = 0; i < answerArray.size(); i++) {
                            cn.hutool.json.JSONObject item = answerArray.getJSONObject(i);
                            Long qId = item.getLong("questionId");
                            String ans = item.getStr("answer");
                            if (qId != null && ans != null) {
                                answerMap.put(qId, ans);
                            }
                        }
                    } else if (answersStr.trim().startsWith("{")) {
                        // 对象格式：{"q_180":"B", "q_182":"A"...}
                        cn.hutool.json.JSONObject answersObj = cn.hutool.json.JSONUtil.parseObj(answersStr);
                        for (String key : answersObj.keySet()) {
                            if (key.startsWith("q_")) {
                                try {
                                    Long qId = Long.parseLong(key.substring(2));
                                    answerMap.put(qId, answersObj.getStr(key));
                                } catch (NumberFormatException e) {
                                    // 忽略格式错误的key
                                }
                            }
                        }
                    } else {
                        continue; // 未知格式
                    }
                } catch (Exception e) {
                    continue; // 解析失败
                }
                
                // 判断是否已阅卷
                boolean isGraded = record.getStatus() != null && record.getStatus() >= 2;
                
                // 遍历题目，找出错题
                for (cn.hutool.json.JSONObject q : questions) {
                    Long questionId = q.getLong("questionId");
                    Question question = questionMapper.selectById(questionId);
                    if (question == null) continue;
                    
                    String studentAnswer = answerMap.get(questionId);
                    if (studentAnswer == null) continue; // 没有作答的题目跳过
                    
                    // 判断答案是否正确
                    boolean isCorrect = false;
                    
                    // 如果已阅卷，根据每题得分判断
                    if (isGraded && record.getDetails() != null) {
                        try {
                            // 解析答题详情，查找该题得分
                            cn.hutool.json.JSONArray details = cn.hutool.json.JSONUtil.parseArray(record.getDetails());
                            for (int i = 0; i < details.size(); i++) {
                                cn.hutool.json.JSONObject detail = details.getJSONObject(i);
                                Long detailQuestionId = detail.getLong("questionId");
                                if (detailQuestionId != null && detailQuestionId.equals(questionId)) {
                                    // 找到该题，判断得分
                                    BigDecimal questionScore = detail.getBigDecimal("score");
                                    BigDecimal questionTotalScore = q.getBigDecimal("score");
                                    if (questionTotalScore != null && questionTotalScore.compareTo(BigDecimal.ZERO) > 0) {
                                        // 得分 == 满分，视为答对
                                        if (questionScore != null && questionScore.compareTo(questionTotalScore) == 0) {
                                            isCorrect = true;
                                        }
                                    }
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            // 解析失败，使用自动判分
                        }
                    }
                    
                    // 如果没有从详情中判断出来，使用自动判分
                    if (!isCorrect && (!isGraded || record.getDetails() == null)) {
                        if (question.getType() == 1) {
                            // 单选题：直接比较
                            isCorrect = question.getAnswer().trim().equalsIgnoreCase(studentAnswer.trim());
                        } else if (question.getType() == 3) {
                            // 判断题：标准化后比较
                            isCorrect = isTrueFalseMatch(question.getAnswer(), studentAnswer);
                        } else if (question.getType() == 2) {
                            // 多选题：排序后比较
                            String sortedStudent = sortAnswer(studentAnswer);
                            String sortedCorrect = sortAnswer(question.getAnswer());
                            isCorrect = sortedStudent.equals(sortedCorrect);
                        } else if (question.getType() == 4) {
                            // 填空题：支持多个答案，包含匹配
                            String[] correctAnswers = question.getAnswer().split("[,|，]");
                            String studentAns = studentAnswer.trim();
                            for (String ans : correctAnswers) {
                                String correctAns = ans.trim();
                                if (studentAns.equalsIgnoreCase(correctAns) || 
                                    studentAns.contains(correctAns) || 
                                    correctAns.contains(studentAns)) {
                                    isCorrect = true;
                                    break;
                                }
                            }
                        } else {
                            // 主观题（简答/编程），如果没有详情数据，跳过
                            continue;
                        }
                    }
                    
                    // 如果答错或需要复习，加入错题本
                    if (!isCorrect) {
                        Map<String, Object> wrongItem = new HashMap<>();
                        wrongItem.put("id", record.getId() + "_" + questionId);
                        wrongItem.put("examName", exam.getExamName());
                        wrongItem.put("subject", paper.getSubject());
                        wrongItem.put("questionContent", question.getContent());
                        wrongItem.put("questionType", question.getType());
                        wrongItem.put("options", question.getOptions());
                        wrongItem.put("studentAnswer", studentAnswer);
                        wrongItem.put("correctAnswer", question.getAnswer());
                        wrongItem.put("analysis", question.getAnalysis());
                        // 格式化时间为字符串
                        if (record.getSubmitTime() != null) {
                            wrongItem.put("submitTime", record.getSubmitTime().toString());
                        }
                        wrongQuestions.add(wrongItem);
                    }
                }
            }
        }
        
        return Result.success(wrongQuestions);
    }
    
    /**
     * 排序答案（用于多选题比较）
     */
    private String sortAnswer(String answer) {
        if (answer == null) return "";
        List<String> parts = Arrays.asList(answer.split(","));
        Collections.sort(parts);
        return String.join(",", parts);
    }
    
    /**
     * 判断题答案匹配（兼容多种格式）
     */
    private boolean isTrueFalseMatch(String correctAnswer, String studentAnswer) {
        String correct = normalizeTrueFalse(correctAnswer);
        String student = normalizeTrueFalse(studentAnswer);
        return correct.equals(student);
    }
    
    /**
     * 将判断题答案标准化为 "T" 或 "F"
     */
    private String normalizeTrueFalse(String answer) {
        if (answer == null) return "";
        String ans = answer.trim().toLowerCase();
        // 正确的各种表示
        if (ans.equals("正确") || ans.equals("√") || ans.equals("t") || ans.equals("true") || ans.equals("1") || ans.equals("对")) {
            return "T";
        }
        // 错误的各种表示
        if (ans.equals("错误") || ans.equals("×") || ans.equals("x") || ans.equals("f") || ans.equals("false") || ans.equals("0") || ans.equals("错")) {
            return "F";
        }
        return ans;
    }
    
    /**
     * 获取个人中心学习统计数据
     */
    @GetMapping("/profile/stats")
    public Result<Map<String, Object>> getProfileStats(@RequestAttribute("userId") Long userId) {
        // 获取学生信息
        Student student = studentMapper.selectByUserId(userId);
        if (student == null) {
            return Result.error("学生信息不存在");
        }
        
        Map<String, Object> stats = new HashMap<>();
        
        // 查询学生的考试记录
        List<ExamRecord> records = examRecordMapper.selectList(null, student.getId());
        
        if (records == null || records.isEmpty()) {
            stats.put("examCount", 0);
            stats.put("avgScore", 0);
            stats.put("maxScore", 0);
            stats.put("passRate", 0);
            stats.put("wrongCount", 0);
            stats.put("weakPoints", "");
            return Result.success(stats);
        }
        
        // 计算参加考试次数（已提交的考试）
        long examCount = records.stream()
            .filter(r -> r.getStatus() != null && (r.getStatus() == 1 || r.getStatus() == 2))
            .count();
        stats.put("examCount", examCount);
        
        // 计算平均成绩和最高成绩
        List<Double> scores = records.stream()
            .filter(r -> r.getStatus() != null && r.getStatus() == 2 && r.getScore() != null)
            .map(r -> r.getScore().doubleValue())
            .collect(Collectors.toList());
        
        if (!scores.isEmpty()) {
            double avgScore = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double maxScore = scores.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            stats.put("avgScore", Math.round(avgScore * 10) / 10.0);
            stats.put("maxScore", maxScore);
            
            // 计算及格率（60分及格）
            long passCount = scores.stream().filter(s -> s >= 60).count();
            double passRate = (passCount * 100.0) / scores.size();
            stats.put("passRate", Math.round(passRate));
        } else {
            stats.put("avgScore", 0);
            stats.put("maxScore", 0);
            stats.put("passRate", 0);
        }
        
        // 统计错题数量（需要分析答卷）
        int wrongCount = 0;
        Map<String, Integer> knowledgePointErrors = new HashMap<>();
        
        for (ExamRecord record : records) {
            if (record.getStatus() == null || record.getAnswers() == null) continue;
            
            try {
                Exam exam = examMapper.selectById(record.getExamId());
                if (exam == null || exam.getPaperId() == null) continue;
                
                Paper paper = paperMapper.selectById(exam.getPaperId());
                if (paper == null || paper.getQuestionConfig() == null) continue;
                
                // 解析试卷题目配置
                cn.hutool.json.JSONObject config = cn.hutool.json.JSONUtil.parseObj(paper.getQuestionConfig());
                cn.hutool.json.JSONArray questions = config.getJSONArray("questions");
                if (questions == null) continue;
                
                // 解析学生答案
                cn.hutool.json.JSONArray studentAnswers = cn.hutool.json.JSONUtil.parseArray(record.getAnswers());
                
                // 统计错题
                for (int i = 0; i < questions.size(); i++) {
                    cn.hutool.json.JSONObject questionConfig = questions.getJSONObject(i);
                    Long questionId = questionConfig.getLong("questionId");
                    
                    // 获取正确答案
                    Question question = questionMapper.selectById(questionId);
                    if (question == null) continue;
                    
                    // 查找学生答案
                    String studentAnswer = null;
                    for (int j = 0; j < studentAnswers.size(); j++) {
                        cn.hutool.json.JSONObject answer = studentAnswers.getJSONObject(j);
                        if (answer.getLong("questionId").equals(questionId)) {
                            studentAnswer = answer.getStr("answer");
                            break;
                        }
                    }
                    
                    // 判断是否答错
                    if (studentAnswer != null) {
                        String correctAnswer = question.getAnswer();
                        boolean isCorrect = false;
                        
                        if (question.getType() == 3) { // 多选题
                            isCorrect = sortAnswer(studentAnswer).equals(sortAnswer(correctAnswer));
                        } else {
                            isCorrect = studentAnswer.equals(correctAnswer);
                        }
                        
                        if (!isCorrect) {
                            wrongCount++;
                            // 统计知识点错误
                            if (question.getKnowledgePoint() != null) {
                                String kp = question.getKnowledgePoint();
                                knowledgePointErrors.put(kp, knowledgePointErrors.getOrDefault(kp, 0) + 1);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        stats.put("wrongCount", wrongCount);
        
        // 找出薄弱知识点（错误最多的3个）
        String weakPoints = knowledgePointErrors.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(3)
            .map(Map.Entry::getKey)
            .collect(Collectors.joining("、"));
        stats.put("weakPoints", weakPoints);
        
        return Result.success(stats);
    }
}
