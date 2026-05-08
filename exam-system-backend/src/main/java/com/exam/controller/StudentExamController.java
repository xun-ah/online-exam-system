package com.exam.controller;

import cn.hutool.json.JSONUtil;
import com.exam.common.Result;
import com.exam.entity.*;
import com.exam.mapper.*;
import com.exam.service.StudentExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/student/exams")
public class StudentExamController {
    
    @Autowired
    private StudentExamService studentExamService;
    
    @Autowired
    private ExamMapper examMapper;
    
    @Autowired
    private PaperMapper paperMapper;
    
    @Autowired
    private QuestionMapper questionMapper;
    
    @Autowired
    private ExamRecordMapper examRecordMapper;
    
    @Autowired
    private StudentMapper studentMapper;
    
    /**
     * 获取考试详情（试卷题目）
     */
    @GetMapping("/{examId}")
    public Result<Map<String, Object>> getExamDetail(@PathVariable Long examId,
                                                     @RequestAttribute("userId") Long userId) {
        // 获取考试信息
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            return Result.error("考试不存在");
        }
        
        // 获取学生信息
        Student student = studentMapper.selectByUserId(userId);
        if (student == null) {
            return Result.error("学生信息不存在");
        }
        
        // 检查是否已经提交过考试
        List<ExamRecord> existingRecords = examRecordMapper.selectList(examId, student.getId());
        if (existingRecords != null && !existingRecords.isEmpty()) {
            ExamRecord existingRecord = existingRecords.get(0);
            // 如果状态是2（已提交），则不允许再次考试
            if (existingRecord.getStatus() != null && existingRecord.getStatus() == 2) {
                return Result.error("您已经参加过该考试");
            }
        }
        
        // 获取试卷信息
        Paper paper = paperMapper.selectById(exam.getPaperId());
        if (paper == null) {
            return Result.error("试卷不存在");
        }
        
        // 解析试卷题目配置
        Map<String, Object> result = new HashMap<>();
        result.put("examId", exam.getId());
        result.put("examName", exam.getExamName());
        result.put("paperName", paper.getPaperName());
        result.put("duration", exam.getDuration());
        result.put("totalScore", paper.getTotalScore());
        // 返回结束时间，用于前端计算剩余时间
        if (exam.getEndTime() != null) {
            result.put("endTime", exam.getEndTime().toString());
        }
        
        // 获取题目列表
        List<Map<String, Object>> questions = studentExamService.getExamQuestions(paper.getQuestionConfig());
        result.put("questions", questions);
        
        return Result.success(result);
    }
    
    /**
     * 开始考试（创建考试记录）
     */
    @PostMapping("/{examId}/start")
    public Result<Map<String, Object>> startExam(@PathVariable Long examId,
                                                 @RequestAttribute("userId") Long userId) {
        // 获取学生信息
        Student student = studentMapper.selectByUserId(userId);
        if (student == null) {
            return Result.error("学生信息不存在");
        }
        
        // 检查是否已经参加过考试
        List<ExamRecord> existingRecords = examRecordMapper.selectList(examId, student.getId());
        if (existingRecords != null && !existingRecords.isEmpty()) {
            ExamRecord existingRecord = existingRecords.get(0);
            // 如果状态是2（已提交），则不允许再次考试
            if (existingRecord.getStatus() != null && existingRecord.getStatus() == 2) {
                return Result.error("您已经参加过该考试");
            }
            // 如果状态是1（考试中），则返回已有的记录ID
            if (existingRecord.getStatus() != null && existingRecord.getStatus() == 1) {
                Map<String, Object> data = new HashMap<>();
                data.put("recordId", existingRecord.getId());
                // 返回ISO格式的本地时间字符串
                data.put("startTime", existingRecord.getCreateTime().toString());
                data.put("alreadyStarted", true);
                return Result.success(data);
            }
        }
        
        // 创建考试记录
        ExamRecord record = new ExamRecord();
        record.setExamId(examId);
        record.setStudentId(student.getId());
        record.setStatus(1); // 1-考试中
        record.setCreateTime(LocalDateTime.now());
        examRecordMapper.insert(record);
        
        Map<String, Object> data = new HashMap<>();
        data.put("recordId", record.getId());
        // 返回ISO格式的本地时间字符串，前端可以直接解析
        data.put("startTime", record.getCreateTime().toString());
        
        return Result.success(data);
    }
    
    /**
     * 保存答案（临时保存）
     */
    @PostMapping("/save-answer")
    public Result<Void> saveAnswer(@RequestBody Map<String, Object> params,
                                   @RequestAttribute("userId") Long userId) {
        try {
            Long recordId = ((Number) params.get("recordId")).longValue();
            String answersJson = (String) params.get("answers");
            
            // 获取学生信息
            Student student = studentMapper.selectByUserId(userId);
            if (student == null) {
                return Result.error("学生信息不存在");
            }
            
            // 查询考试记录
            ExamRecord record = examRecordMapper.selectById(recordId);
            if (record == null || !record.getStudentId().equals(student.getId())) {
                return Result.error("考试记录不存在");
            }
            
            // 将答案保存到exam_record的answers字段
            record.setAnswers(answersJson);
            examRecordMapper.updateById(record);
            
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("保存失败：" + e.getMessage());
        }
    }
    
    /**
     * 提交试卷
     */
    @PostMapping("/{examId}/submit")
    public Result<Void> submitExam(@PathVariable Long examId,
                                   @RequestBody Map<String, Object> params,
                                   @RequestAttribute("userId") Long userId) {
        // 获取学生信息
        Student student = studentMapper.selectByUserId(userId);
        if (student == null) {
            return Result.error("学生信息不存在");
        }
        
        // 获取考试记录
        List<ExamRecord> records = examRecordMapper.selectList(examId, student.getId());
        if (records == null || records.isEmpty()) {
            return Result.error("考试记录不存在");
        }
        
        ExamRecord record = records.get(0);
        
        // 获取答案
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answers = (List<Map<String, Object>>) params.get("answers");
        if (answers != null) {
            // 将答案转换为JSON字符串保存
            record.setAnswers(JSONUtil.toJsonStr(answers));
            
            // 自动阅卷（客观题）
            BigDecimal objectiveScore = autoGradeObjectiveQuestions(examId, answers);
            record.setScore(objectiveScore);
            record.setStatus(1); // 1-已提交（待阅卷）
        } else {
            record.setStatus(1);
        }
        
        record.setSubmitTime(LocalDateTime.now());
        examRecordMapper.updateById(record);
        
        return Result.success();
    }
    
    /**
     * 自动批改客观题（单选、多选、判断）
     */
    private BigDecimal autoGradeObjectiveQuestions(Long examId, List<Map<String, Object>> studentAnswers) {
        // 获取考试信息
        Exam exam = examMapper.selectById(examId);
        if (exam == null) return BigDecimal.ZERO;
        
        // 获取试卷信息
        Paper paper = paperMapper.selectById(exam.getPaperId());
        if (paper == null) return BigDecimal.ZERO;
        
        // 解析试卷题目配置
        String questionConfig = paper.getQuestionConfig();
        if (questionConfig == null || questionConfig.isEmpty()) return BigDecimal.ZERO;
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questionDetails = (List<Map<String, Object>>) JSONUtil.parseObj(questionConfig).get("questions");
        if (questionDetails == null || questionDetails.isEmpty()) return BigDecimal.ZERO;
        
        BigDecimal totalScore = BigDecimal.ZERO;
        
        // 遍历题目配置，逐个判分
        for (Map<String, Object> questionDetail : questionDetails) {
            Long questionId = Long.valueOf(questionDetail.get("questionId").toString());
            BigDecimal questionScore = new BigDecimal(questionDetail.get("score").toString());
            
            // 获取题目信息
            Question question = questionMapper.selectById(questionId);
            if (question == null) continue;
            
            // 只处理客观题：1-单选题 2-多选题 3-判断题
            if (question.getType() < 1 || question.getType() > 3) continue;
            
            // 查找学生答案
            String studentAnswer = null;
            for (Map<String, Object> answer : studentAnswers) {
                if (Long.valueOf(answer.get("questionId").toString()).equals(questionId)) {
                    studentAnswer = answer.get("answer").toString();
                    break;
                }
            }
            
            // 如果学生没有作答，跳过
            if (studentAnswer == null || studentAnswer.trim().isEmpty()) continue;
            
            // 判分逻辑
            boolean isCorrect = false;
            
            if (question.getType() == 1) {
                // 单选题：直接比较答案
                isCorrect = question.getAnswer().trim().equalsIgnoreCase(studentAnswer.trim());
            } else if (question.getType() == 3) {
                // 判断题：兼容多种答案格式
                String correctAns = question.getAnswer().trim();
                String studentAns = studentAnswer.trim();
                
                // 将答案统一转换为标准格式进行比较
                isCorrect = isTrueFalseMatch(correctAns, studentAns);
            } else if (question.getType() == 2) {
                // 多选题：比较答案（不区分顺序）
                String correctAnswer = question.getAnswer().trim().toUpperCase();
                String studentAns = studentAnswer.trim().toUpperCase();
                
                // 将答案转为Set比较
                Set<String> correctSet = new HashSet<>(Arrays.asList(correctAnswer.split("")));
                Set<String> studentSet = new HashSet<>(Arrays.asList(studentAns.split("")));
                
                isCorrect = correctSet.equals(studentSet);
            }
            
            // 答对加分
            if (isCorrect) {
                totalScore = totalScore.add(questionScore);
            }
        }
        
        return totalScore;
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
