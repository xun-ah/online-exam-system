package com.exam.controller;

import com.exam.common.Result;
import com.exam.entity.*;
import com.exam.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 成绩分析Controller
 */
@RestController
@RequestMapping("/teacher/score-analysis")
public class ScoreAnalysisController {
    
    @Autowired
    private ExamMapper examMapper;
    
    @Autowired
    private ExamRecordMapper examRecordMapper;
    
    @Autowired
    private StudentMapper studentMapper;
    
    @Autowired
    private ClassMapper classMapper;
    
    @Autowired
    private PaperMapper paperMapper;
    
    @Autowired
    private QuestionMapper questionMapper;
    
    /**
     * 获取班级间成绩对比
     */
    @GetMapping("/class-comparison")
    public Result<List<Map<String, Object>>> getClassComparison(@RequestParam Long examId) {
        try {
            // 获取该考试的所有考试记录
            List<ExamRecord> records = examRecordMapper.selectList(examId, null);
            if (records == null || records.isEmpty()) {
                return Result.success(new ArrayList<>());
            }
            
            // 按班级分组统计
            Map<Long, List<ExamRecord>> classRecordsMap = new HashMap<>();
            for (ExamRecord record : records) {
                if (record.getStatus() == null || record.getStatus() < 2) continue;
                
                Student student = studentMapper.selectById(record.getStudentId());
                if (student == null || student.getClassId() == null) continue;
                
                classRecordsMap.computeIfAbsent(student.getClassId(), k -> new ArrayList<>()).add(record);
            }
            
            // 统计每个班级的成绩
            List<Map<String, Object>> comparison = new ArrayList<>();
            for (Map.Entry<Long, List<ExamRecord>> entry : classRecordsMap.entrySet()) {
                Long classId = entry.getKey();
                List<ExamRecord> classRecords = entry.getValue();
                
                ClassInfo classInfo = classMapper.selectById(classId);
                if (classInfo == null) continue;
                
                List<BigDecimal> scores = classRecords.stream()
                    .map(ExamRecord::getScore)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
                
                if (scores.isEmpty()) continue;
                
                double avg = scores.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
                double max = scores.stream().mapToDouble(BigDecimal::doubleValue).max().orElse(0);
                double min = scores.stream().mapToDouble(BigDecimal::doubleValue).min().orElse(0);
                long passCount = scores.stream().filter(s -> s.doubleValue() >= 60).count();
                double passRate = (double) passCount / scores.size() * 100;
                
                Map<String, Object> classData = new HashMap<>();
                classData.put("classId", classId);
                classData.put("className", classInfo.getClassName());
                classData.put("studentCount", scores.size());
                classData.put("avgScore", Math.round(avg * 10) / 10.0);
                classData.put("maxScore", max);
                classData.put("minScore", min);
                classData.put("passRate", Math.round(passRate * 10) / 10.0);
                
                comparison.add(classData);
            }
            
            // 按平均分排序
            comparison.sort((a, b) -> Double.compare((Double)b.get("avgScore"), (Double)a.get("avgScore")));
            
            return Result.success(comparison);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取班级对比失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取知识点掌握度分析
     */
    @GetMapping("/knowledge-analysis")
    public Result<Map<String, Object>> getKnowledgeAnalysis(@RequestParam Long examId) {
        try {
            // 获取考试信息
            Exam exam = examMapper.selectById(examId);
            if (exam == null) {
                return Result.error("考试不存在");
            }
            
            // 获取试卷信息
            Paper paper = paperMapper.selectById(exam.getPaperId());
            if (paper == null || paper.getQuestionConfig() == null) {
                return Result.error("试卷信息不完整");
            }
            
            // 解析试卷题目配置
            cn.hutool.json.JSONObject config = cn.hutool.json.JSONUtil.parseObj(paper.getQuestionConfig());
            cn.hutool.json.JSONArray questionsArray = config.getJSONArray("questions");
            
            // 统计各知识点的答题情况
            Map<String, Map<String, Object>> knowledgeStats = new LinkedHashMap<>();
            
            for (int i = 0; i < questionsArray.size(); i++) {
                cn.hutool.json.JSONObject qConfig = questionsArray.getJSONObject(i);
                Long questionId = qConfig.getLong("questionId");
                
                Question question = questionMapper.selectById(questionId);
                if (question == null) {
                    continue;
                }
                
                // 如果知识点为空，使用题目内容前 10 个字作为知识点
                String knowledgePoint = question.getKnowledgePoint();
                if (knowledgePoint == null || knowledgePoint.isEmpty()) {
                    knowledgePoint = "未设置知识点";
                }
                knowledgeStats.putIfAbsent(knowledgePoint, new HashMap<String, Object>() {{
                    put("totalCount", 0);
                    put("correctCount", 0);
                    put("questions", new ArrayList<String>());
                }});
                
                Map<String, Object> stats = knowledgeStats.get(knowledgePoint);
                stats.put("totalCount", (int)stats.get("totalCount") + 1);
                
                @SuppressWarnings("unchecked")
                List<String> questions = (List<String>) stats.get("questions");
                questions.add(question.getContent().substring(0, Math.min(30, question.getContent().length())));
            }
            
            // 统计每个知识点的正确率
            List<ExamRecord> records = examRecordMapper.selectList(examId, null).stream()
                .filter(r -> r.getStatus() != null && r.getStatus() >= 2 && r.getAnswers() != null)
                .collect(Collectors.toList());
                        
            for (ExamRecord record : records) {
                try {
                    cn.hutool.json.JSONArray answersArray = cn.hutool.json.JSONUtil.parseArray(record.getAnswers());
                    for (int i = 0; i < answersArray.size(); i++) {
                        cn.hutool.json.JSONObject ansObj = answersArray.getJSONObject(i);
                        Long questionId = Long.valueOf(ansObj.get("questionId").toString());
                        String studentAnswer = ansObj.get("answer").toString();
                                    
                        Question question = questionMapper.selectById(questionId);
                        if (question == null) continue;
                                    
                        // 如果知识点为空，使用题目内容前 10 个字作为知识点
                        String knowledgePoint = question.getKnowledgePoint();
                        if (knowledgePoint == null || knowledgePoint.isEmpty()) {
                            knowledgePoint = "未设置知识点";
                        }
                        Map<String, Object> stats = knowledgeStats.get(knowledgePoint);
                        if (stats == null) continue;
                                    
                        // 判断是否正确
                        boolean isCorrect = false;
                        if (question.getType() == 1) {
                            // 单选题：直接比较答案
                            isCorrect = question.getAnswer().trim().equalsIgnoreCase(studentAnswer.trim());
                        } else if (question.getType() == 3) {
                            // 判断题：兼容多种格式（对/错、T/F、1/0、正确/错误）
                            String correctAns = question.getAnswer().trim().toUpperCase();
                            String studentAns = studentAnswer.trim().toUpperCase();
                            // 标准化答案
                            if (correctAns.equals("T") || correctAns.equals("TRUE") || correctAns.equals("1") || correctAns.equals("对") || correctAns.equals("正确")) {
                                correctAns = "T";
                            } else {
                                correctAns = "F";
                            }
                            if (studentAns.equals("T") || studentAns.equals("TRUE") || studentAns.equals("1") || studentAns.equals("对") || studentAns.equals("正确")) {
                                studentAns = "T";
                            } else {
                                studentAns = "F";
                            }
                            isCorrect = correctAns.equals(studentAns);
                        } else if (question.getType() == 2) {
                            // 多选题：排序后比较
                            isCorrect = sortAnswer(question.getAnswer()).equals(sortAnswer(studentAnswer));
                        } else if (question.getType() == 4) {
                            // 填空题：精确匹配
                            isCorrect = question.getAnswer().trim().equalsIgnoreCase(studentAnswer.trim());
                        } else if (question.getType() == 5 || question.getType() == 6) {
                            // 简答题和编程题：只要提交了非空答案就算答对
                            isCorrect = studentAnswer != null && !studentAnswer.trim().isEmpty() && !studentAnswer.equals("未作答");
                        }
                                    
                        if (isCorrect) {
                            stats.put("correctCount", (int)stats.get("correctCount") + 1);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            // 构建返回数据
            List<Map<String, Object>> knowledgeList = new ArrayList<>();
            for (Map.Entry<String, Map<String, Object>> entry : knowledgeStats.entrySet()) {
                Map<String, Object> data = new HashMap<>();
                data.put("knowledgePoint", entry.getKey());
                int totalCount = (int)entry.getValue().get("totalCount") * records.size();
                int correctCount = (int)entry.getValue().get("correctCount");
                data.put("accuracy", totalCount > 0 ? Math.round((double)correctCount / totalCount * 1000) / 10.0 : 0);
                data.put("totalCount", totalCount);
                data.put("correctCount", correctCount);
                data.put("questions", entry.getValue().get("questions"));
                knowledgeList.add(data);
            }
            
            // 按正确率排序
            knowledgeList.sort((a, b) -> Double.compare((Double)a.get("accuracy"), (Double)b.get("accuracy")));
            
            Map<String, Object> result = new HashMap<>();
            result.put("knowledgeList", knowledgeList);
            result.put("totalKnowledgePoints", knowledgeList.size());
            
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取知识点分析失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取试题质量分析（难度、区分度）
     */
    @GetMapping("/question-quality")
    public Result<Map<String, Object>> getQuestionQuality(@RequestParam Long examId) {
        try {
            // 获取考试信息
            Exam exam = examMapper.selectById(examId);
            if (exam == null) {
                return Result.error("考试不存在");
            }
            
            // 获取试卷信息
            Paper paper = paperMapper.selectById(exam.getPaperId());
            if (paper == null || paper.getQuestionConfig() == null) {
                return Result.error("试卷信息不完整");
            }
            
            // 解析试卷题目配置
            cn.hutool.json.JSONObject config = cn.hutool.json.JSONUtil.parseObj(paper.getQuestionConfig());
            cn.hutool.json.JSONArray questionsArray = config.getJSONArray("questions");
            
            // 获取所有考试记录
            List<ExamRecord> records = examRecordMapper.selectList(examId, null).stream()
                .filter(r -> r.getStatus() != null && r.getStatus() >= 2 && r.getAnswers() != null)
                .collect(Collectors.toList());
            
            if (records.isEmpty()) {
                return Result.error("暂无答题数据");
            }
            
            // 分析每道题的质量
            List<Map<String, Object>> questionAnalysis = new ArrayList<>();
            
            for (int i = 0; i < questionsArray.size(); i++) {
                cn.hutool.json.JSONObject qConfig = questionsArray.getJSONObject(i);
                Long questionId = qConfig.getLong("questionId");
                BigDecimal score = new BigDecimal(qConfig.get("score").toString());
                
                Question question = questionMapper.selectById(questionId);
                if (question == null) continue;
                
                // 只分析客观题
                if (question.getType() > 3) continue;
                
                // 统计该题的答题情况
                int totalAnswered = 0;
                int correctCount = 0;
                
                for (ExamRecord record : records) {
                    try {
                        cn.hutool.json.JSONArray answersArray = cn.hutool.json.JSONUtil.parseArray(record.getAnswers());
                        for (int j = 0; j < answersArray.size(); j++) {
                            cn.hutool.json.JSONObject ansObj = answersArray.getJSONObject(j);
                            Long ansQuestionId = Long.valueOf(ansObj.get("questionId").toString());
                            
                            if (ansQuestionId.equals(questionId)) {
                                totalAnswered++;
                                String studentAnswer = ansObj.get("answer").toString();
                                
                                boolean isCorrect = false;
                                if (question.getType() == 1 || question.getType() == 3) {
                                    isCorrect = question.getAnswer().trim().equalsIgnoreCase(studentAnswer.trim());
                                } else if (question.getType() == 2) {
                                    isCorrect = sortAnswer(question.getAnswer()).equals(sortAnswer(studentAnswer));
                                }
                                
                                if (isCorrect) {
                                    correctCount++;
                                }
                                break;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                
                // 计算难度系数 (答对率) - 保持0-1范围，前端显示时转换
                double difficulty = totalAnswered > 0 ? (double)correctCount / totalAnswered : 0;
                
                // 计算区分度 (简化版: 高分组答对率 - 低分组答对率)
                double discrimination = calculateDiscrimination(questionId, records);
                
                Map<String, Object> analysis = new HashMap<>();
                analysis.put("questionId", questionId);
                analysis.put("content", question.getContent().substring(0, Math.min(50, question.getContent().length())));
                analysis.put("type", question.getType());
                // 转换为百分比显示 (0-100)
                analysis.put("difficulty", Math.round(difficulty * 100));
                analysis.put("discrimination", Math.round(discrimination * 100));
                analysis.put("correctRate", Math.round(difficulty * 100));
                analysis.put("totalAnswered", totalAnswered);
                analysis.put("correctCount", correctCount);
                
                // 难度等级
                String difficultyLevel;
                if (difficulty >= 0.7) {
                    difficultyLevel = "简单";
                } else if (difficulty >= 0.4) {
                    difficultyLevel = "中等";
                } else {
                    difficultyLevel = "困难";
                }
                analysis.put("difficultyLevel", difficultyLevel);
                
                // 区分度评价
                String discriminationLevel;
                if (discrimination >= 0.4) {
                    discriminationLevel = "优秀";
                } else if (discrimination >= 0.3) {
                    discriminationLevel = "良好";
                } else if (discrimination >= 0.2) {
                    discriminationLevel = "一般";
                } else {
                    discriminationLevel = "较差";
                }
                analysis.put("discriminationLevel", discriminationLevel);
                
                questionAnalysis.add(analysis);
            }
            
            // 统计整体质量 - 注意：这里计算的是已经转换为百分比的平均值
            double avgDifficulty = questionAnalysis.stream()
                .mapToDouble(q -> ((Number)q.get("difficulty")).doubleValue())
                .average().orElse(0);
            
            double avgDiscrimination = questionAnalysis.stream()
                .mapToDouble(q -> ((Number)q.get("discrimination")).doubleValue())
                .average().orElse(0);
            
            Map<String, Object> result = new HashMap<>();
            result.put("questions", questionAnalysis);
            // 直接取平均值，因为已经是百分比
            result.put("avgDifficulty", Math.round(avgDifficulty * 10) / 10.0);
            result.put("avgDiscrimination", Math.round(avgDiscrimination * 10) / 10.0);
            result.put("totalQuestions", questionAnalysis.size());
            
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取试题质量分析失败: " + e.getMessage());
        }
    }
    
    /**
     * 计算区分度（高分组答对率 - 低分组答对率）
     */
    private double calculateDiscrimination(Long questionId, List<ExamRecord> records) {
        try {
            // 按总分排序
            List<ExamRecord> sortedRecords = records.stream()
                .filter(r -> r.getScore() != null)
                .sorted((a, b) -> b.getScore().compareTo(a.getScore()))
                .collect(Collectors.toList());
            
            if (sortedRecords.size() < 4) return 0;
            
            // 取前27%为高分组，后27%为低分组
            int groupSize = (int) (sortedRecords.size() * 0.27);
            groupSize = Math.max(1, groupSize);
            
            List<ExamRecord> highGroup = sortedRecords.subList(0, groupSize);
            List<ExamRecord> lowGroup = sortedRecords.subList(sortedRecords.size() - groupSize, sortedRecords.size());
            
            // 统计高分组答对率
            int highCorrect = countCorrect(questionId, highGroup);
            double highRate = (double) highCorrect / highGroup.size();
            
            // 统计低分组答对率
            int lowCorrect = countCorrect(questionId, lowGroup);
            double lowRate = (double) lowCorrect / lowGroup.size();
            
            return highRate - lowRate;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * 统计某题在指定记录中的答对数
     */
    private int countCorrect(Long questionId, List<ExamRecord> records) {
        int count = 0;
        for (ExamRecord record : records) {
            try {
                cn.hutool.json.JSONArray answersArray = cn.hutool.json.JSONUtil.parseArray(record.getAnswers());
                for (int i = 0; i < answersArray.size(); i++) {
                    cn.hutool.json.JSONObject ansObj = answersArray.getJSONObject(i);
                    Long ansQuestionId = Long.valueOf(ansObj.get("questionId").toString());
                    
                    if (ansQuestionId.equals(questionId)) {
                        Question question = questionMapper.selectById(questionId);
                        if (question == null) break;
                        
                        String studentAnswer = ansObj.get("answer").toString();
                        boolean isCorrect = false;
                        
                        if (question.getType() == 1 || question.getType() == 3) {
                            isCorrect = question.getAnswer().trim().equalsIgnoreCase(studentAnswer.trim());
                        } else if (question.getType() == 2) {
                            isCorrect = sortAnswer(question.getAnswer()).equals(sortAnswer(studentAnswer));
                        }
                        
                        if (isCorrect) count++;
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return count;
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
