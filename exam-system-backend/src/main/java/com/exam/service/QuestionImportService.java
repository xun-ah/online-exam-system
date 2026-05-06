package com.exam.service;

import com.exam.entity.Question;
import com.exam.mapper.QuestionMapper;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QuestionImportService {

    @Autowired
    private QuestionMapper questionMapper;

    /**
     * 从Word文件批量导入题目
     * 支持的格式：
     * 一、单选题
     * 1. 题目内容
     * A. 选项A
     * B. 选项B
     * C. 选项C
     * D. 选项D
     * 答案：A
     * 解析：解析内容（可选）
     */
    public int importQuestionsFromWord(MultipartFile file, Long teacherId, String subject) throws Exception {
        List<Question> questions = new ArrayList<>();
        
        try (InputStream inputStream = file.getInputStream()) {
            XWPFDocument document = new XWPFDocument(inputStream);
            
            // 提取所有文本
            StringBuilder fullText = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                fullText.append(paragraph.getText()).append("\n");
            }
            
            String text = fullText.toString();
            questions = parseQuestions(text, teacherId, subject);
        }
        
        // 批量插入数据库
        int successCount = 0;
        for (Question question : questions) {
            try {
                // 生成题目编号
                if (question.getQuestionNo() == null || question.getQuestionNo().isEmpty()) {
                    question.setQuestionNo(generateQuestionNo(teacherId));
                }
                questionMapper.insert(question);
                successCount++;
            } catch (Exception e) {
                // 记录错误但继续导入其他题目
                System.err.println("导入题目失败: " + question.getContent() + ", 错误: " + e.getMessage());
            }
        }
        
        return successCount;
    }

    /**
     * 解析题目文本
     */
    private List<Question> parseQuestions(String text, Long teacherId, String subject) {
        List<Question> questions = new ArrayList<>();
        
        // 按题型分割（一、单选题 / 二、多选题 等）
        String[] sections = text.split("([一二三四五六七八九十]+)、");
        
        for (String section : sections) {
            if (section.trim().isEmpty()) continue;
            
            // 提取题型
            Integer type = extractQuestionType(section);
            if (type == null) continue;
            
            // 分割各个题目
            String[] questionBlocks = section.split("\n\\d+\\.");
            
            for (int i = 1; i < questionBlocks.length; i++) { // 从1开始，跳过题型标题
                String block = questionBlocks[i].trim();
                if (block.isEmpty()) continue;
                
                Question question = parseSingleQuestion(block, type, teacherId, subject);
                if (question != null) {
                    questions.add(question);
                }
            }
        }
        
        return questions;
    }

    /**
     * 解析单个题目
     */
    private Question parseSingleQuestion(String block, Integer type, Long teacherId, String subject) {
        try {
            Question question = new Question();
            question.setType(type);
            question.setTeacherId(teacherId);
            question.setSubject(subject); // 设置科目
            question.setDifficulty(2); // 默认中等难度
            question.setScore(new BigDecimal("5")); // 默认5分
            
            String[] lines = block.split("\n");
            StringBuilder contentBuilder = new StringBuilder();
            StringBuilder answerBuilder = new StringBuilder();
            StringBuilder analysisBuilder = new StringBuilder();
            StringBuilder optionsBuilder = new StringBuilder();
            
            boolean inOptions = false;
            
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // 检查是否是答案行
                if (line.matches("答案[：:].*")) {
                    String answer = line.replaceAll("答案[：:]", "").trim();
                    answerBuilder.append(answer);
                    continue;
                }
                
                // 检查是否是解析行
                if (line.matches("(解析|答案解析)[：:].*")) {
                    String analysis = line.replaceAll("(解析|答案解析)[：:]", "").trim();
                    analysisBuilder.append(analysis);
                    continue;
                }
                
                // 检查是否是选项
                if (line.matches("[A-D][.．、].*")) {
                    String option = line.replaceFirst("[A-D][.．、]", "").trim();
                    char optionChar = line.charAt(0);
                    if (optionsBuilder.length() > 0) {
                        optionsBuilder.append(",");
                    }
                    optionsBuilder.append(optionChar).append(":").append(option);
                    inOptions = true;
                    continue;
                }
                
                // 其他行作为题目内容
                if (!inOptions || !line.matches("^[A-D].*")) {
                    if (contentBuilder.length() > 0) {
                        contentBuilder.append("\n");
                    }
                    contentBuilder.append(line);
                    inOptions = false;
                }
            }
            
            question.setContent(contentBuilder.toString());
            question.setAnswer(answerBuilder.toString());
            question.setAnalysis(analysisBuilder.toString());
            
            // 如果有选项，转换为JSON格式
            if (optionsBuilder.length() > 0) {
                question.setOptions("{" + optionsBuilder + "}");
            }
            
            return question;
        } catch (Exception e) {
            System.err.println("解析题目失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从文本中提取题型
     */
    private Integer extractQuestionType(String section) {
        String firstLine = section.split("\n")[0].trim();
        
        if (firstLine.contains("单选题")) return 1;
        if (firstLine.contains("多选题")) return 2;
        if (firstLine.contains("判断题")) return 3;
        if (firstLine.contains("填空题")) return 4;
        if (firstLine.contains("简答题")) return 5;
        
        return null;
    }

    /**
     * 生成题目编号
     */
    private String generateQuestionNo(Long teacherId) {
        long timestamp = System.currentTimeMillis();
        String timestampSuffix = String.valueOf(timestamp).substring(7);
        return "Q" + teacherId + timestampSuffix;
    }
}
