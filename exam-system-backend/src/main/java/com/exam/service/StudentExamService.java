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
     * @param questionConfig 试卷题目配置
     * @param shuffleEnabled 是否启用乱序 0-否 1-是
     */
    public List<Map<String, Object>> getExamQuestions(String questionConfig, Integer shuffleEnabled) {
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
            
            // 1. 题目乱序：打乱题目顺序（仅在启用乱序时）
            List<JSONObject> questionsList = questionsArray.toList(JSONObject.class);
            if (shuffleEnabled != null && shuffleEnabled == 1) {
                Collections.shuffle(questionsList);
            }
            
            int questionNumber = 1;
            
            for (JSONObject questionItem : questionsList) {
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
                            
                            // 2. 选项乱序：收集非空选项并打乱顺序
                            List<Map.Entry<String, String>> optionsEntries = new ArrayList<>();
                            if (optionsJson.containsKey("A") && optionsJson.getStr("A") != null) 
                                optionsEntries.add(new AbstractMap.SimpleEntry<>("A", optionsJson.getStr("A")));
                            if (optionsJson.containsKey("B") && optionsJson.getStr("B") != null) 
                                optionsEntries.add(new AbstractMap.SimpleEntry<>("B", optionsJson.getStr("B")));
                            if (optionsJson.containsKey("C") && optionsJson.getStr("C") != null) 
                                optionsEntries.add(new AbstractMap.SimpleEntry<>("C", optionsJson.getStr("C")));
                            if (optionsJson.containsKey("D") && optionsJson.getStr("D") != null) 
                                optionsEntries.add(new AbstractMap.SimpleEntry<>("D", optionsJson.getStr("D")));
                            
                            Collections.shuffle(optionsEntries);
                            
                            // 将乱序后的选项按顺序赋给 A, B, C, D
                            String[] keys = {"A", "B", "C", "D"};
                            for (int i = 0; i < Math.min(optionsEntries.size(), 4); i++) {
                                questionData.put("option" + keys[i], optionsEntries.get(i).getValue());
                            }
                        } catch (Exception e) {
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
