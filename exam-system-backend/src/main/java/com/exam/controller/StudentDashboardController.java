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
            
            if (now.isBefore(exam.getStartTime())) {
                pendingCount++;
            } else if (now.isAfter(exam.getEndTime())) {
                completedCount++;
            }
        }
        
        stats.put("pendingCount", pendingCount);
        stats.put("completedCount", completedCount);
        stats.put("avgScore", 82.5); // TODO: 需要从成绩表计算真实平均分
        stats.put("classRank", 5); // TODO: 需要从成绩表计算真实排名
        
        return Result.success(stats);
    }

    /**
     * 获取待考考试列表
     */
    @GetMapping("/exams/pending")
    public Result<List<Map<String, Object>>> getPendingExams(@RequestAttribute("userId") Long userId) {
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
        
        // 根据班级ID查询考试列表（只显示该班级的考试）
        List<Exam> exams = examMapper.selectListByClassId(student.getClassId());
        if (exams == null || exams.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        
        // 过滤未删除的考试，并计算动态状态
        List<Map<String, Object>> pendingExams = exams.stream()
            .filter(exam -> exam.getDeleted() == null || exam.getDeleted() == 0)
            .map(exam -> {
                // 如果学生已经参加过该考试，标记为已完成
                if (completedExamIds.contains(exam.getId())) {
                    Map<String, Object> examData = new HashMap<>();
                    examData.put("id", exam.getId());
                    examData.put("examName", exam.getExamName());
                    examData.put("paperName", exam.getPaperName());
                    examData.put("startTime", exam.getStartTime());
                    examData.put("endTime", exam.getEndTime());
                    examData.put("duration", exam.getDuration());
                    examData.put("className", exam.getClassName());
                    examData.put("status", 2); // 已完成
                    examData.put("completed", true);
                    return examData;
                }
                
                // 计算考试状态
                LocalDateTime now = LocalDateTime.now();
                int status;
                if (now.isBefore(exam.getStartTime())) {
                    status = 0; // 未开始
                } else if (!now.isBefore(exam.getStartTime()) && !now.isAfter(exam.getEndTime())) {
                    status = 1; // 进行中（包含边界时间）
                } else {
                    status = 2; // 已结束
                }
                
                // 确保 duration 不为 null，如果为 null 则根据 startTime 和 endTime 计算
                Integer duration = exam.getDuration();
                if (duration == null && exam.getStartTime() != null && exam.getEndTime() != null) {
                    duration = (int) java.time.Duration.between(exam.getStartTime(), exam.getEndTime()).toMinutes();
                }
                
                Map<String, Object> examData = new HashMap<>();
                examData.put("id", exam.getId());
                examData.put("examName", exam.getExamName());
                examData.put("paperName", exam.getPaperName());
                examData.put("startTime", exam.getStartTime());
                examData.put("endTime", exam.getEndTime());
                examData.put("duration", duration);
                examData.put("className", exam.getClassName());
                examData.put("status", status);
                examData.put("completed", false);
                
                return examData;
            })
            .collect(Collectors.toList());
        
        return Result.success(pendingExams);
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
                data.put("duration", exam.getDuration());
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
    public Result<List<Map<String, Object>>> getRecentExams(@RequestAttribute("userId") Long userId) {
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
        
        // 获取考试信息
        List<Exam> exams = examMapper.selectListByDepartmentId(student.getDepartmentId());
        Map<Long, Exam> examMap = new HashMap<>();
        if (exams != null) {
            for (Exam exam : exams) {
                examMap.put(exam.getId(), exam);
            }
        }
        
        // 组装数据，只取最近5条已提交的记录
        List<Map<String, Object>> recentList = records.stream()
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
            .limit(5)
            .collect(Collectors.toList());
        
        return Result.success(recentList);
    }

    /**
     * 获取成绩趋势
     */
    @GetMapping("/score/trend")
    public Result<List<Map<String, Object>>> getScoreTrend(@RequestAttribute("userId") Long userId) {
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
        
        // 获取考试信息
        List<Exam> exams = examMapper.selectListByDepartmentId(student.getDepartmentId());
        Map<Long, Exam> examMap = new HashMap<>();
        if (exams != null) {
            for (Exam exam : exams) {
                examMap.put(exam.getId(), exam);
            }
        }
        
        // 组装数据，只取已提交的记录
        List<Map<String, Object>> trendList = records.stream()
            .filter(record -> record.getStatus() != null && record.getStatus() == 2) // 已提交
            .map(record -> {
                Exam exam = examMap.get(record.getExamId());
                if (exam == null) {
                    return null;
                }
                
                Map<String, Object> data = new HashMap<>();
                data.put("examName", exam.getExamName());
                data.put("score", record.getScore());
                
                return data;
            })
            .filter(Objects::nonNull)
            .sorted((a, b) -> Long.compare((Long) b.get("id"), (Long) a.get("id"))) // 按时间顺序
            .limit(10) // 最多显示10次考试
            .collect(Collectors.toList());
        
        return Result.success(trendList);
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
        
        // 根据院系生成对应的知识点数据（使用固定分数）
        List<Map<String, Object>> radarData = generateKnowledgePointsByDepartment(student.getDepartmentName());
        
        return Result.success(radarData);
    }
    
    /**
     * 根据院系生成知识点数据（使用固定分数，避免每次刷新变化）
     */
    private List<Map<String, Object>> generateKnowledgePointsByDepartment(String departmentName) {
        List<Map<String, Object>> points = new ArrayList<>();
        
        if (departmentName == null) {
            return points;
        }
        
        // 根据院系返回对应的知识点，使用固定分数
        if (departmentName.contains("数学")) {
            points.add(createRadarItem("高等数学", 72));
            points.add(createRadarItem("线性代数", 68));
            points.add(createRadarItem("概率统计", 65));
            points.add(createRadarItem("离散数学", 61));
            points.add(createRadarItem("数值分析", 63));
            points.add(createRadarItem("数学建模", 66));
        } else if (departmentName.contains("外国语")) {
            points.add(createRadarItem("听力理解", 70));
            points.add(createRadarItem("阅读理解", 68));
            points.add(createRadarItem("写作表达", 64));
            points.add(createRadarItem("翻译能力", 62));
            points.add(createRadarItem("语法词汇", 66));
            points.add(createRadarItem("口语交际", 69));
        } else if (departmentName.contains("计算机") || departmentName.contains("电子信息")) {
            points.add(createRadarItem("数据结构", 71));
            points.add(createRadarItem("算法设计", 65));
            points.add(createRadarItem("操作系统", 63));
            points.add(createRadarItem("计算机网络", 67));
            points.add(createRadarItem("数据库原理", 69));
            points.add(createRadarItem("软件工程", 64));
        } else if (departmentName.contains("经济管理")) {
            points.add(createRadarItem("微观经济学", 70));
            points.add(createRadarItem("宏观经济学", 67));
            points.add(createRadarItem("管理学原理", 68));
            points.add(createRadarItem("会计学基础", 65));
            points.add(createRadarItem("市场营销", 66));
            points.add(createRadarItem("财务管理", 63));
        } else if (departmentName.contains("法学")) {
            points.add(createRadarItem("宪法学", 72));
            points.add(createRadarItem("民法学", 68));
            points.add(createRadarItem("刑法学", 65));
            points.add(createRadarItem("行政法学", 63));
            points.add(createRadarItem("经济法学", 66));
            points.add(createRadarItem("诉讼法学", 64));
        } else if (departmentName.contains("汉语言")) {
            points.add(createRadarItem("古代文学", 73));
            points.add(createRadarItem("现代文学", 70));
            points.add(createRadarItem("语言学", 66));
            points.add(createRadarItem("写作", 68));
            points.add(createRadarItem("文学理论", 64));
            points.add(createRadarItem("文献学", 62));
        } else {
            // 默认通用知识点
            points.add(createRadarItem("基础知识", 65));
            points.add(createRadarItem("应用能力", 62));
            points.add(createRadarItem("分析能力", 68));
            points.add(createRadarItem("实践能力", 64));
            points.add(createRadarItem("创新能力", 66));
            points.add(createRadarItem("综合能力", 63));
        }
        
        return points;
    }
    
    private Map<String, Object> createRadarItem(String name, int score) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("score", score);
        return item;
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
                
                // 解析学生答案
                cn.hutool.json.JSONObject answers = cn.hutool.json.JSONUtil.parseObj(record.getAnswers());
                
                // 遍历题目，找出错题
                for (cn.hutool.json.JSONObject q : questions) {
                    Long questionId = q.getLong("questionId");
                    Question question = questionMapper.selectById(questionId);
                    if (question == null) continue;
                    
                    // 只处理客观题（单选、多选、判断）
                    if (question.getType() == 4 || question.getType() == 5) continue;
                    
                    String studentAnswer = answers.getStr("q_" + questionId);
                    if (studentAnswer == null) continue;
                    
                    // 判断答案是否正确
                    boolean isCorrect = false;
                    if (question.getType() == 1 || question.getType() == 3) {
                        // 单选题或判断题：直接比较
                        isCorrect = question.getAnswer().trim().equalsIgnoreCase(studentAnswer.trim());
                    } else if (question.getType() == 2) {
                        // 多选题：排序后比较
                        String sortedStudent = sortAnswer(studentAnswer);
                        String sortedCorrect = sortAnswer(question.getAnswer());
                        isCorrect = sortedStudent.equals(sortedCorrect);
                    }
                    
                    // 如果答错，加入错题本
                    if (!isCorrect) {
                        Map<String, Object> wrongItem = new HashMap<>();
                        wrongItem.put("id", record.getId() + "_" + questionId);
                        wrongItem.put("examName", exam.getExamName());
                        wrongItem.put("questionContent", question.getContent());
                        wrongItem.put("questionType", question.getType());
                        wrongItem.put("studentAnswer", studentAnswer);
                        wrongItem.put("correctAnswer", question.getAnswer());
                        wrongItem.put("analysis", question.getAnalysis());
                        wrongItem.put("submitTime", record.getSubmitTime());
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
}
