package com.exam.controller;

import cn.hutool.json.JSONUtil;
import com.exam.common.Result;
import com.exam.entity.*;
import com.exam.mapper.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
     * 获取已阅卷列表（支持分页）
     */
    @GetMapping("/graded")
    public Result<Map<String, Object>> getGradedRecords(
            @RequestParam(required = false) Long examId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
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
        
        // 实现分页
        int total = gradedList.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        
        List<Map<String, Object>> pageList;
        if (fromIndex < total) {
            pageList = gradedList.subList(fromIndex, toIndex);
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
                                // 主观题（简答题、填空题、编程题）
                                subjectiveTotal += questionScore.intValue();
                                Map<String, Object> qData = new HashMap<>();
                                qData.put("id", question.getId());
                                qData.put("number", subjectiveNumber++);
                                String typeText;
                                if (question.getType() == 4) {
                                    typeText = "填空题";
                                } else if (question.getType() == 6) {
                                    typeText = "编程题";
                                } else {
                                    typeText = "简答题";
                                }
                                qData.put("type", typeText);
                                qData.put("content", question.getContent());
                                qData.put("fullScore", questionScore.intValue());
                                qData.put("studentAnswer", studentAnswer);
                                // 添加参考答案（简答题的answer字段作为参考）
                                qData.put("referenceAnswer", question.getAnswer());
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
        
        // 保存每道主观题的详细评分和评语（可选功能，需要扩展表结构）
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> subjectiveQuestions = (List<Map<String, Object>>) params.get("subjectiveQuestions");
        if (subjectiveQuestions != null && !subjectiveQuestions.isEmpty()) {
            // TODO: 如果需要保存每题详细评分，可以创建 exam_record_detail 表存储
            System.out.println("收到 " + subjectiveQuestions.size() + " 道主观题的评分详情");
            for (Map<String, Object> q : subjectiveQuestions) {
                System.out.println("题目ID: " + q.get("questionId") + ", 得分: " + q.get("score") + ", 评语: " + q.get("comment"));
            }
        }
        
        return Result.success("评分提交成功", null);
    }
    
    /**
     * 打回考试记录（让学生重做）
     */
    @PostMapping("/{recordId}/rollback")
    public Result<Void> rollbackExamRecord(@PathVariable Long recordId,
                                           @RequestBody Map<String, String> params,
                                           @RequestAttribute("userId") Long userId) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null) {
            return Result.error("考试记录不存在");
        }
        
        // 获取打回原因（可选）
        String reason = params != null ? params.getOrDefault("reason", "") : "";
        
        // 重置记录状态为0（未提交/可重考），清空分数和答案
        record.setStatus(0);
        record.setScore(null);
        record.setAnswers(null); // 清空答案，让学生重新答题
        examRecordMapper.updateById(record);
        
        System.out.println("打回考试记录 - recordId: " + recordId + ", reason: " + reason);
        
        return Result.success("打回成功，学生可以重新考试", null);
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
     * 导出成绩为Excel文件
     */
    @GetMapping("/export")
    public void exportScores(@RequestParam(required = false) Long examId,
                             @RequestAttribute("userId") Long userId,
                             HttpServletResponse response) {
        // 获取教师所属院系ID
        Teacher teacher = teacherMapper.selectByUserId(userId);
        if (teacher == null) {
            response.setStatus(400);
            return;
        }
        
        List<ExamRecord> records;
        Exam targetExam = null;
        
        if (examId != null) {
            // 如果指定了考试ID，直接查询该考试的记录
            records = examRecordMapper.selectList(examId, null);
            targetExam = examMapper.selectById(examId);
        } else {
            // 如果没有指定考试ID，查询本院系所有考试的记录
            List<Exam> exams = examMapper.selectListByDepartmentId(teacher.getDepartmentId());
            records = new ArrayList<>();
            for (Exam exam : exams) {
                List<ExamRecord> examRecords = examRecordMapper.selectList(exam.getId(), null);
                records.addAll(examRecords);
            }
        }
        
        if (records == null || records.isEmpty()) {
            response.setStatus(404);
            return;
        }
        
        try {
            // 创建Excel工作簿
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("成绩表");
            
            // 创建标题样式
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            
            // 创建内容样式
            CellStyle contentStyle = workbook.createCellStyle();
            contentStyle.setBorderBottom(BorderStyle.THIN);
            contentStyle.setBorderTop(BorderStyle.THIN);
            contentStyle.setBorderLeft(BorderStyle.THIN);
            contentStyle.setBorderRight(BorderStyle.THIN);
            
            // 创建表头行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"序号", "学号", "姓名", "考试名称", "班级", "客观题得分", "主观题得分", "总分", "提交时间", "状态"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 填充数据
            int rowNum = 1;
            for (ExamRecord record : records) {
                // 只导出已提交的记录
                if (record.getStatus() == null || record.getStatus() < 1) continue;
                
                Student student = studentMapper.selectById(record.getStudentId());
                if (student == null) continue;
                
                // 获取考试信息
                Exam exam = targetExam != null ? targetExam : examMapper.selectById(record.getExamId());
                String examName = exam != null ? exam.getExamName() : "";
                
                // 获取班级信息
                String className = "";
                if (exam != null && exam.getClassId() != null) {
                    ClassInfo classInfo = classMapper.selectById(exam.getClassId());
                    className = classInfo != null ? classInfo.getClassName() : "";
                }
                
                // 计算客观题和主观题得分
                int objectiveScore = 0;
                int subjectiveScore = 0;
                if (record.getStatus() == 2 && record.getScore() != null) {
                    // 已阅卷，客观题得分从score字段获取
                    objectiveScore = record.getScore().intValue();
                    // 主观题得分需要计算
                    subjectiveScore = exam != null && exam.getPaperId() != null ? 
                        calculateSubjectiveScore(exam.getPaperId(), record) : 0;
                }
                
                // 创建数据行
                Row row = sheet.createRow(rowNum++);
                
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(rowNum - 1);
                cell0.setCellStyle(contentStyle);
                
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(student.getStudentNo());
                cell1.setCellStyle(contentStyle);
                
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(student.getRealName());
                cell2.setCellStyle(contentStyle);
                
                Cell cell3 = row.createCell(3);
                cell3.setCellValue(examName);
                cell3.setCellStyle(contentStyle);
                
                Cell cell4 = row.createCell(4);
                cell4.setCellValue(className);
                cell4.setCellStyle(contentStyle);
                
                Cell cell5 = row.createCell(5);
                cell5.setCellValue(objectiveScore);
                cell5.setCellStyle(contentStyle);
                
                Cell cell6 = row.createCell(6);
                cell6.setCellValue(subjectiveScore);
                cell6.setCellStyle(contentStyle);
                
                Cell cell7 = row.createCell(7);
                cell7.setCellValue(record.getScore() != null ? record.getScore().doubleValue() : 0);
                cell7.setCellStyle(contentStyle);
                
                Cell cell8 = row.createCell(8);
                cell8.setCellValue(record.getSubmitTime() != null ? 
                    record.getSubmitTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
                cell8.setCellStyle(contentStyle);
                
                Cell cell9 = row.createCell(9);
                cell9.setCellValue(record.getStatus() == 1 ? "待阅卷" : "已阅卷");
                cell9.setCellStyle(contentStyle);
            }
            
            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // 设置最大宽度
                if (sheet.getColumnWidth(i) > 256 * 50) {
                    sheet.setColumnWidth(i, 256 * 50);
                }
            }
            
            // 设置响应头
            String fileName = "成绩表_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()));
            
            // 写入响应输出流
            try (OutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
                outputStream.flush();
            }
            
            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
        }
    }
    
    /**
     * 计算主观题得分
     */
    private int calculateSubjectiveScore(Long paperId, ExamRecord record) {
        try {
            Paper paper = paperMapper.selectById(paperId);
            if (paper == null || paper.getQuestionConfig() == null) return 0;
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> questionDetails = (List<Map<String, Object>>) JSONUtil.parseObj(paper.getQuestionConfig()).get("questions");
            
            int subjectiveScore = 0;
            
            for (Map<String, Object> q : questionDetails) {
                Long qId = Long.valueOf(q.get("questionId").toString());
                Question question = questionMapper.selectById(qId);
                
                // 只计算主观题（简答题 type=5）
                if (question != null && question.getType() == 5) {
                    // 主观题得分需要从阅卷记录中获取，这里简化处理
                    // 实际应该从教师的阅卷评分中获取
                    subjectiveScore += 0; // 默认0分，需要教师手动阅卷
                }
            }
            
            return subjectiveScore;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
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
