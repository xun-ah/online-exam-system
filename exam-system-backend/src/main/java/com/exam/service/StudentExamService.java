package com.exam.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.exam.entity.Question;
import com.exam.mapper.QuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class StudentExamService {
    
    @Autowired
    private QuestionMapper questionMapper;
    
    /**
     * 根据试卷配置获取题目列表
     */
    public List<Map<String, Object>> getExamQuestions(String questionConfig) {
        if (questionConfig == null || questionConfig.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            JSONObject config = JSONUtil.parseObj(questionConfig);
            JSONArray questionsArray = config.getJSONArray("questions");
            
            if (questionsArray == null || questionsArray.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<Map<String, Object>> result = new ArrayList<>();
            int questionNumber = 1;
            
            for (int i = 0; i < questionsArray.size(); i++) {
                JSONObject questionItem = questionsArray.getJSONObject(i);
                Long questionId = questionItem.getLong("questionId");
                BigDecimal score = questionItem.getBigDecimal("score");
                
                // 获取题目详情
                Question question = questionMapper.selectById(questionId);
                if (question != null) {
                    Map<String, Object> questionData = new HashMap<>();
                    questionData.put("questionNumber", questionNumber++);
                    questionData.put("questionId", question.getId());
                    questionData.put("content", question.getContent());
                    questionData.put("type", question.getType());
                    questionData.put("difficulty", question.getDifficulty());
                    questionData.put("score", score);
                    
                    // 解析选项（从JSON格式）
                    if (question.getOptions() != null && !question.getOptions().isEmpty()) {
                        try {
                            JSONObject optionsJson = JSONUtil.parseObj(question.getOptions());
                            questionData.put("optionA", optionsJson.getStr("A"));
                            questionData.put("optionB", optionsJson.getStr("B"));
                            questionData.put("optionC", optionsJson.getStr("C"));
                            questionData.put("optionD", optionsJson.getStr("D"));
                        } catch (Exception e) {
                            // 如果解析失败，记录日志但不阻塞
                            System.err.println("解析选项失败，题目ID: " + questionId + ", 选项数据: " + question.getOptions());
                            e.printStackTrace();
                        }
                    }
                    
                    // 不返回答案，防止作弊
                    
                    result.add(questionData);
                }
            }
            
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
