package com.exam.controller;

import cn.hutool.json.JSONUtil;
import com.exam.common.Result;
import com.exam.entity.*;
import com.exam.mapper.*;
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
    
    @Autowired
    private ClassMapper classMapper;
    
    @Autowired
    private PaperMapper paperMapper;
    
    @Autowired
    private QuestionMapper questionMapper;
    
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
        
        // 查询本院系的所有考试
        List<Exam> exams = examMapper.selectListByDepartmentId(teacher.getDepartmentId());
        List<Map<String, Object>> pendingList = new ArrayList<>();
        
        for (Exam exam : exams) {
            List<ExamRecord> records = examRecordMapper.selectList(exam.getId(), null);
            for (ExamRecord record : records) {
                // status=0 或 status=1 表示待阅卷（根据实际业务定义）
                if (record.getStatus() != null && record.getStatus() == 1) {
                    Student student = studentMapper.selectById(record.getStudentId());
                    
                    // 获取班级名称
                    String className = "";
                    if (exam.getClassId() != null) {
                        ClassInfo classInfo = classMapper.selectById(exam.getClassId());
                        className = classInfo != null ? classInfo.getClassName() : "";
                    }
                    
                    // 计算客观题和主观题满分
                    int objectiveTotal = 0;
                    int subjectiveTotal = 0;
                    if (exam.getPaperId() != null) {
                        Paper paper = paperMapper.selectById(exam.getPaperId());
                        if (paper != null && paper.getQuestionConfig() != null) {
                            try {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> questionDetails = (List<Map<String, Object>>) JSONUtil.parseObj(paper.getQuestionConfig()).get("questions");
                                if (questionDetails != null) {
                                    for (Map<String, Object> q : questionDetails) {
                                        Long qId = Long.valueOf(q.get("questionId").toString());
                                        BigDecimal score = new BigDecimal(q.get("score").toString());
                                        Question question = questionMapper.selectById(qId);
                                        if (question != null) {
                                            if (question.getType() <= 3) {
                                                objectiveTotal += score.intValue();
                                            } else {
                                                subjectiveTotal += score.intValue();
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    
                    Map<String, Object> data = new HashMap<>();
                    data.put("recordId", record.getId());
                    data.put("examId", exam.getId());
                    data.put("examName", exam.getExamName());
                    data.put("className", className);
                    data.put("pendingCount", 1);
                    data.put("objectiveTotal", objectiveTotal);
                    data.put("subjectiveTotal", subjectiveTotal);
                    data.put("submitTime", record.getSubmitTime() != null ? 
                        record.getSubmitTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
                    pendingList.add(data);
                }
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
            
            // 获取考试信息以计算满分和得分
            Exam exam = examMapper.selectById(record.getExamId());
            int objectiveTotal = 0;
            int subjectiveTotal = 0;
            int objectiveScore = 0;
            int subjectiveScore = 0;
            if (exam != null && exam.getPaperId() != null) {
                Paper paper = paperMapper.selectById(exam.getPaperId());
                if (paper != null && paper.getQuestionConfig() != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> questionDetails = (List<Map<String, Object>>) JSONUtil.parseObj(paper.getQuestionConfig()).get("questions");
                        if (questionDetails != null) {
                            // 解析学生答案
                            Map<Long, String> studentAnswers = new HashMap<>();
                            if (record.getAnswers() != null) {
                                try {
                                    cn.hutool.json.JSONArray answersArray = JSONUtil.parseArray(record.getAnswers());
                                    for (int i = 0; i < answersArray.size(); i++) {
                                        cn.hutool.json.JSONObject ansObj = answersArray.getJSONObject(i);
                                        Long qId = Long.valueOf(ansObj.get("questionId").toString());
                                        studentAnswers.put(qId, ansObj.get("answer").toString());
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            
                            for (Map<String, Object> q : questionDetails) {
                                Long qId = Long.valueOf(q.get("questionId").toString());
                                BigDecimal score = new BigDecimal(q.get("score").toString());
                                Question question = questionMapper.selectById(qId);
                                if (question != null) {
                                    if (question.getType() <= 3) {
                                        // 客观题
                                        objectiveTotal += score.intValue();
                                        String studentAnswer = studentAnswers.get(qId);
                                        if (studentAnswer != null) {
                                            boolean isCorrect = false;
                                            if (question.getType() == 1) {
                                                // 单选题
                                                isCorrect = question.getAnswer().trim().equalsIgnoreCase(studentAnswer.trim());
                                            } else if (question.getType() == 3) {
                                                // 判断题：兼容多种答案格式
                                                isCorrect = isTrueFalseMatch(question.getAnswer(), studentAnswer);
                                            } else if (question.getType() == 2) {
                                                // 多选题：比较答案（不区分顺序）
                                                String correctAnswer = question.getAnswer().trim().toUpperCase();
                                                String studentAns = studentAnswer.trim().toUpperCase();
                                                Set<String> correctSet = new HashSet<>(Arrays.asList(correctAnswer.split("")));
                                                Set<String> studentSet = new HashSet<>(Arrays.asList(studentAns.split("")));
                                                isCorrect = correctSet.equals(studentSet);
                                            }
                                            if (isCorrect) {
                                                objectiveScore += score.intValue();
                                            }
                                        }
                                    } else {
                                        // 主观题
                                        subjectiveTotal += score.intValue();
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            
            // 如果已阅卷，从数据库读取客观题得分
            if (record.getStatus() != null && record.getStatus() == 2 && record.getScore() != null) {
                objectiveScore = record.getScore().intValue();
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("id", record.getId());
            data.put("studentNo", student != null ? student.getStudentNo() : "");
            data.put("studentName", student != null ? student.getRealName() : "");
            data.put("realName", student != null ? student.getRealName() : "");
            data.put("objectiveScore", objectiveScore);
            data.put("objectiveTotal", objectiveTotal);
            data.put("subjectiveScore", subjectiveScore);
            data.put("subjectiveTotal", subjectiveTotal);
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
        
        // 获取考试和班级信息
        Exam exam = examMapper.selectById(record.getExamId());
        String className = "";
        if (exam != null && exam.getClassId() != null) {
            ClassInfo classInfo = classMapper.selectById(exam.getClassId());
            className = classInfo != null ? classInfo.getClassName() : "";
        }
        
        List<Map<String, Object>> objectiveQuestions = new ArrayList<>();
        List<Map<String, Object>> subjectiveQuestions = new ArrayList<>();
        int objectiveTotal = 0;
        int subjectiveTotal = 0;
        int subjectiveScore = 0;
        
        if (exam != null && exam.getPaperId() != null) {
            Paper paper = paperMapper.selectById(exam.getPaperId());
            if (paper != null && paper.getQuestionConfig() != null) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> questionDetails = (List<Map<String, Object>>) JSONUtil.parseObj(paper.getQuestionConfig()).get("questions");
                    if (questionDetails != null) {
                        // 解析学生答案
                        Map<Long, String> studentAnswers = new HashMap<>();
                        if (record.getAnswers() != null) {
                            try {
                                cn.hutool.json.JSONArray answersArray = JSONUtil.parseArray(record.getAnswers());
                                for (int i = 0; i < answersArray.size(); i++) {
                                    cn.hutool.json.JSONObject ansObj = answersArray.getJSONObject(i);
                                    Long qId = Long.valueOf(ansObj.get("questionId").toString());
                                    studentAnswers.put(qId, ansObj.get("answer").toString());
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        
                        int objectiveNumber = 1;
                        int subjectiveNumber = 1;
                        
                        for (Map<String, Object> questionDetail : questionDetails) {
                            Long questionId = Long.valueOf(questionDetail.get("questionId").toString());
                            BigDecimal questionScore = new BigDecimal(questionDetail.get("score").toString());
                            
                            Question question = questionMapper.selectById(questionId);
                            if (question == null) continue;
                            
                            String studentAnswer = studentAnswers.getOrDefault(questionId, "未作答");
                            
                            if (question.getType() <= 3) {
                                // 客观题
                                objectiveTotal += questionScore.intValue();
                                boolean isCorrect = false;
                                
                                if (question.getType() == 1) {
                                    // 单选题
                                    isCorrect = question.getAnswer().trim().equalsIgnoreCase(studentAnswer.trim());
                                } else if (question.getType() == 3) {
                                    // 判断题：兼容多种答案格式
                                    isCorrect = isTrueFalseMatch(question.getAnswer(), studentAnswer);
                                } else if (question.getType() == 2) {
                                    // 多选题
                                    String correctAnswer = question.getAnswer().trim().toUpperCase();
                                    String studentAns = studentAnswer.trim().toUpperCase();
                                    Set<String> correctSet = new HashSet<>(Arrays.asList(correctAnswer.split("")));
                                    Set<String> studentSet = new HashSet<>(Arrays.asList(studentAns.split("")));
                                    isCorrect = correctSet.equals(studentSet);
                                }
                                
                                Map<String, Object> qData = new HashMap<>();
                                qData.put("number", objectiveNumber++);
                                qData.put("type", question.getType() == 1 ? "单选题" : (question.getType() == 2 ? "多选题" : "判断题"));
                                qData.put("content", question.getContent());
                                qData.put("correctAnswer", question.getAnswer());
                                qData.put("studentAnswer", studentAnswer);
                                qData.put("fullScore", questionScore.intValue());
                                qData.put("isCorrect", isCorrect);
                                objectiveQuestions.add(qData);
                            } else {
                                // 主观题
                                subjectiveTotal += questionScore.intValue();
                                Map<String, Object> qData = new HashMap<>();
                                qData.put("id", question.getId());
                                qData.put("number", subjectiveNumber++);
                                qData.put("type", question.getType() == 4 ? "填空题" : "简答题");
                                qData.put("content", question.getContent());
                                qData.put("fullScore", questionScore.intValue());
                                qData.put("studentAnswer", studentAnswer);
                                qData.put("score", 0);
                                subjectiveQuestions.add(qData);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        Map<String, Object> detail = new HashMap<>();
        detail.put("recordId", record.getId());
        detail.put("studentNo", student != null ? student.getStudentNo() : "");
        detail.put("studentName", student != null ? student.getRealName() : "");
        detail.put("className", className);
        detail.put("objectiveScore", record.getScore() != null ? record.getScore().intValue() : 0);
        detail.put("objectiveTotal", objectiveTotal);
        detail.put("subjectiveScore", subjectiveScore);
        detail.put("subjectiveTotal", subjectiveTotal);
        detail.put("totalScore", record.getScore() != null ? record.getScore().intValue() : 0);
        detail.put("objectiveQuestions", objectiveQuestions);
        detail.put("subjectiveQuestions", subjectiveQuestions);
        
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
        
        // 获取主观题得分
        BigDecimal subjectiveScore = new BigDecimal(params.getOrDefault("subjectiveScore", "0").toString());
        
        // 获取客观题得分（学生提交时已自动计算）
        BigDecimal objectiveScore = record.getScore() != null ? record.getScore() : BigDecimal.ZERO;
        
        // 计算总分 = 客观题得分 + 主观题得分
        BigDecimal totalScore = objectiveScore.add(subjectiveScore);
        record.setScore(totalScore);
        
        // 更新状态为已阅卷
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
    
    /**
     * 获取错题分析 - 错误率TOP5
     */
    @GetMapping("/error-analysis/{examId}")
    public Result<List<Map<String, Object>>> getErrorAnalysis(@PathVariable Long examId) {
        // 获取该考试的所有已阅卷记录
        List<ExamRecord> records = examRecordMapper.selectList(examId, null);
        List<ExamRecord> graded = records.stream()
            .filter(r -> r.getStatus() != null && r.getStatus() == 2)
            .collect(Collectors.toList());
        
        if (graded.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        
        // 获取试卷信息
        Exam exam = examMapper.selectById(examId);
        if (exam == null || exam.getPaperId() == null) {
            return Result.success(new ArrayList<>());
        }
        
        Paper paper = paperMapper.selectById(exam.getPaperId());
        if (paper == null || paper.getQuestionConfig() == null) {
            return Result.success(new ArrayList<>());
        }
        
        try {
            // 解析题目配置
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> questionDetails = (List<Map<String, Object>>) JSONUtil.parseObj(paper.getQuestionConfig()).get("questions");
            
            // 统计每道题的错误情况
            Map<Long, Map<String, Object>> questionErrorStats = new HashMap<>();
            
            for (Map<String, Object> q : questionDetails) {
                Long qId = Long.valueOf(q.get("questionId").toString());
                Question question = questionMapper.selectById(qId);
                if (question == null) continue;
                
                // 只统计客观题（单选、多选、判断）
                if (question.getType() > 3) continue;
                
                int totalAnswered = 0;
                int errorCount = 0;
                
                // 遍历所有已阅卷记录
                for (ExamRecord record : graded) {
                    if (record.getAnswers() == null) continue;
                    
                    try {
                        cn.hutool.json.JSONArray answersArray = JSONUtil.parseArray(record.getAnswers());
                        for (int i = 0; i < answersArray.size(); i++) {
                            cn.hutool.json.JSONObject ansObj = answersArray.getJSONObject(i);
                            Long recordQId = Long.valueOf(ansObj.get("questionId").toString());
                            
                            if (recordQId.equals(qId)) {
                                totalAnswered++;
                                String studentAnswer = ansObj.get("answer").toString();
                                
                                // 判断是否答错
                                boolean isCorrect = false;
                                if (question.getType() == 1) {
                                    // 单选题
                                    isCorrect = question.getAnswer().trim().equalsIgnoreCase(studentAnswer.trim());
                                } else if (question.getType() == 3) {
                                    // 判断题
                                    isCorrect = isTrueFalseMatch(question.getAnswer(), studentAnswer);
                                } else if (question.getType() == 2) {
                                    // 多选题
                                    isCorrect = question.getAnswer().trim().equalsIgnoreCase(studentAnswer.trim());
                                }
                                
                                if (!isCorrect) {
                                    errorCount++;
                                }
                                break;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                
                if (totalAnswered > 0) {
                    Map<String, Object> errorStat = new HashMap<>();
                    errorStat.put("questionId", qId);
                    errorStat.put("content", question.getContent());
                    errorStat.put("totalCount", totalAnswered);
                    errorStat.put("errorCount", errorCount);
                    errorStat.put("errorRate", String.format("%.1f", (double) errorCount / totalAnswered * 100));
                    questionErrorStats.put(qId, errorStat);
                }
            }
            
            // 按错误率排序，取TOP5
            List<Map<String, Object>> topErrors = questionErrorStats.values().stream()
                .sorted((a, b) -> Double.valueOf(b.get("errorRate").toString()).compareTo(Double.valueOf(a.get("errorRate").toString())))
                .limit(5)
                .collect(Collectors.toList());
            
            return Result.success(topErrors);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取错题分析失败");
        }
    }
    
    /**
     * 判断题答案匹配（兼容多种格式）
     * 支持的格式：正确/错误、√/×、T/F、true/false、1/0
     */
    private boolean isTrueFalseMatch(String correctAnswer, String studentAnswer) {
        // 将答案转换为统一的标准格式
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
        
        // 如果都不匹配，返回原值（用于直接比较）
        return ans;
    }
}
