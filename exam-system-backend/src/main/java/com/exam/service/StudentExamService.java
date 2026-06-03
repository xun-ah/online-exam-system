package com.exam.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.exam.entity.Question;
import com.exam.mapper.QuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.*;

@Service
public class StudentExamService {

    @Autowired
    private QuestionMapper questionMapper;

    /**
     * 根据试卷配置获取题目列表
     *
     * @param questionConfig     试卷题目配置
     * @param shuffleEnabled     是否启用乱序 0-否 1-是
     * @param savedQuestionOrder 已保存的题目顺序（如果存在则使用，否则重新生成）
     */
    public List<Map<String, Object>> getExamQuestions(String questionConfig, Integer shuffleEnabled, String savedQuestionOrder) {
        Map<String, Object> result = getExamQuestionsWithOrder(questionConfig, shuffleEnabled, savedQuestionOrder);
        return (List<Map<String, Object>>) result.get("questions");
    }
    
    /**
     * 根据试卷配置获取题目列表和题目顺序
     *
     * @param questionConfig     试卷题目配置
     * @param shuffleEnabled     是否启用乱序 0-否 1-是
     * @param savedQuestionOrder 已保存的题目顺序（如果存在则使用，否则重新生成）
     * @return Map包含: questions-题目列表, questionOrder-题目顺序JSON
     */
    public Map<String, Object> getExamQuestionsWithOrder(String questionConfig, Integer shuffleEnabled, String savedQuestionOrder) {
        Map<String, Object> resultMap = new HashMap<>();
        
        if (questionConfig == null || questionConfig.isEmpty()) {
            resultMap.put("questions", new ArrayList<>());
            resultMap.put("questionOrder", null);
            return resultMap;
        }

        try {
            JSONObject config = JSONUtil.parseObj(questionConfig);
            JSONArray questionsArray = config.getJSONArray("questions");

            if (questionsArray == null || questionsArray.isEmpty()) {
                resultMap.put("questions", new ArrayList<>());
                resultMap.put("questionOrder", null);
                return resultMap;
            }

            List<Map<String, Object>> result = new ArrayList<>();

            // 1. 按题型分组(创建新列表避免修改原始配置)
            List<JSONObject> singleChoiceList = new ArrayList<>();   // 单选题
            List<JSONObject> multipleChoiceList = new ArrayList<>(); // 多选题
            List<JSONObject> trueFalseList = new ArrayList<>();      // 判断题
            List<JSONObject> fillBlankList = new ArrayList<>();      // 填空题
            List<JSONObject> shortAnswerList = new ArrayList<>();    // 简答题

            for (JSONObject questionItem : questionsArray.toList(JSONObject.class)) {
                // 创建副本避免修改原始数据
                JSONObject copy = JSONUtil.parseObj(questionItem.toString());
                Long questionId = copy.getLong("questionId");
                Question question = questionMapper.selectById(questionId);
                if (question != null) {
                    Integer type = question.getType();
                    if (type != null && type == 1) {
                        singleChoiceList.add(copy); // 单选题
                    } else if (type != null && type == 2) {
                        multipleChoiceList.add(copy); // 多选题
                    } else if (type != null && type == 3) {
                        trueFalseList.add(copy); // 判断题
                    } else if (type != null && type == 4) {
                        fillBlankList.add(copy); // 填空题
                    } else if (type != null && type == 5) {
                        shortAnswerList.add(copy); // 简答题
                    }
                }
            }

            // 2. 各题型独立乱序(每个学生使用不同的随机种子)
            String questionOrderJson = null; // 用于保存题目和选项顺序

            if (shuffleEnabled != null && shuffleEnabled == 1) {
                // 检查是否有保存的题目顺序
                if (savedQuestionOrder != null && !savedQuestionOrder.isEmpty()) {
                    // 使用已保存的题目顺序
                    System.out.println("[乱序] 使用已保存的题目顺序");
                    try {
                        cn.hutool.json.JSONObject savedOrderObj = JSONUtil.parseObj(savedQuestionOrder);
                        cn.hutool.json.JSONArray questionIds = savedOrderObj.getJSONArray("questionIds");
                        cn.hutool.json.JSONObject optionOrders = savedOrderObj.getJSONObject("optionOrders");
                        
                        // 按保存的顺序重新排列题目
                        List<JSONObject> orderedList = new ArrayList<>();
                        for (int i = 0; i < questionIds.size(); i++) {
                            Long qId = questionIds.getLong(i);
                            // 在各题型列表中查找对应的题目
                            for (JSONObject q : singleChoiceList) {
                                if (q.getLong("questionId").equals(qId)) {
                                    orderedList.add(q);
                                    break;
                                }
                            }
                            for (JSONObject q : multipleChoiceList) {
                                if (q.getLong("questionId").equals(qId)) {
                                    orderedList.add(q);
                                    break;
                                }
                            }
                            for (JSONObject q : trueFalseList) {
                                if (q.getLong("questionId").equals(qId)) {
                                    orderedList.add(q);
                                    break;
                                }
                            }
                            for (JSONObject q : fillBlankList) {
                                if (q.getLong("questionId").equals(qId)) {
                                    orderedList.add(q);
                                    break;
                                }
                            }
                            for (JSONObject q : shortAnswerList) {
                                if (q.getLong("questionId").equals(qId)) {
                                    orderedList.add(q);
                                    break;
                                }
                            }
                        }
                        // 清空原列表，使用排序后的列表
                        singleChoiceList.clear();
                        multipleChoiceList.clear();
                        trueFalseList.clear();
                        fillBlankList.clear();
                        shortAnswerList.clear();
                        // 按题型重新分组
                        for (JSONObject q : orderedList) {
                            Question question = questionMapper.selectById(q.getLong("questionId"));
                            if (question != null) {
                                Integer type = question.getType();
                                if (type == 1) singleChoiceList.add(q);
                                else if (type == 2) multipleChoiceList.add(q);
                                else if (type == 3) trueFalseList.add(q);
                                else if (type == 4) fillBlankList.add(q);
                                else if (type == 5) shortAnswerList.add(q);
                            }
                        }
                        
                        // 将选项顺序保存到全局变量中，供后续使用
                        // 注：由于每次调用都会传入 savedQuestionOrder，直接解析即可
                        System.out.println("[乱序] 已加载保存的题目和选项顺序");
                    } catch (Exception e) {
                        System.err.println("[乱序] 解析保存的题目顺序失败，将重新生成");
                        e.printStackTrace();
                    }
                } else {
                    // 生成新的随机顺序
                    SecureRandom secureRandom = new SecureRandom();
                    byte[] seedBytes = new byte[8];
                    secureRandom.nextBytes(seedBytes);
                    long seed = 0;
                    for (byte b : seedBytes) {
                        seed = (seed << 8) | (b & 0xFF);
                    }
                    Random studentRandom = new Random(seed);

                    System.out.println("[乱序] 启用题目和选项乱序 - 学生独立随机(种子:" + seed + ", 时间:" + System.currentTimeMillis() + ")");
                    System.out.println("[乱序] 单选题:" + singleChoiceList.size() + "道，多选题:" + multipleChoiceList.size() + "道，判断题:" + trueFalseList.size() + "道，填空题:" + fillBlankList.size() + "道，简答题:" + shortAnswerList.size() + "道");
                    Collections.shuffle(singleChoiceList, studentRandom);
                    Collections.shuffle(multipleChoiceList, studentRandom);
                    Collections.shuffle(trueFalseList, studentRandom);
                    Collections.shuffle(fillBlankList, studentRandom);
                    Collections.shuffle(shortAnswerList, studentRandom);

                    // 生成题目顺序JSON
                    cn.hutool.json.JSONObject orderObj = new cn.hutool.json.JSONObject();
                    cn.hutool.json.JSONArray orderArray = new cn.hutool.json.JSONArray();
                    cn.hutool.json.JSONObject optionOrdersObj = new cn.hutool.json.JSONObject();
                    
                    for (JSONObject q : singleChoiceList) orderArray.add(q.getLong("questionId"));
                    for (JSONObject q : multipleChoiceList) orderArray.add(q.getLong("questionId"));
                    for (JSONObject q : trueFalseList) orderArray.add(q.getLong("questionId"));
                    for (JSONObject q : fillBlankList) orderArray.add(q.getLong("questionId"));
                    for (JSONObject q : shortAnswerList) orderArray.add(q.getLong("questionId"));
                    
                    orderObj.set("questionIds", orderArray);
                    orderObj.set("optionOrders", optionOrdersObj);
                    questionOrderJson = orderObj.toString();
                    System.out.println("[乱序] 生成的题目顺序: " + questionOrderJson);
                }
            } else {
                System.out.println("[乱序] 未启用乱序，shuffleEnabled=" + shuffleEnabled);
            }

            // 3. 按题型顺序合并：单选 -> 多选 -> 判断 -> 填空 -> 简答
            List<JSONObject> questionsList = new ArrayList<>();
            questionsList.addAll(singleChoiceList);
            questionsList.addAll(multipleChoiceList);
            questionsList.addAll(trueFalseList);
            questionsList.addAll(fillBlankList);
            questionsList.addAll(shortAnswerList);

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

                    // 清理题目内容中的题号前缀（如"12. "、"7. "）
                    String content = question.getContent();
                    if (content != null) {
                        // 移除开头的数字+点+空格格式（如"12. "）
                        content = content.replaceFirst("^\\d+\\.\\s*", "");
                    }
                    questionData.put("content", content);

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

                            if (shuffleEnabled != null && shuffleEnabled == 1) {
                                // 检查是否有保存的选项顺序
                                if (savedQuestionOrder != null && !savedQuestionOrder.isEmpty()) {
                                    try {
                                        cn.hutool.json.JSONObject savedOrderObj = JSONUtil.parseObj(savedQuestionOrder);
                                        cn.hutool.json.JSONObject optionOrders = savedOrderObj.getJSONObject("optionOrders");
                                        if (optionOrders != null && optionOrders.containsKey(String.valueOf(questionId))) {
                                            // 使用已保存的选项顺序
                                            cn.hutool.json.JSONArray savedOptionOrder = optionOrders.getJSONArray(String.valueOf(questionId));
                                            List<Map.Entry<String, String>> restoredOptions = new ArrayList<>();
                                            for (int i = 0; i < savedOptionOrder.size(); i++) {
                                                String key = savedOptionOrder.getStr(i);
                                                for (Map.Entry<String, String> entry : optionsEntries) {
                                                    if (entry.getKey().equals(key)) {
                                                        restoredOptions.add(entry);
                                                        break;
                                                    }
                                                }
                                            }
                                            optionsEntries = restoredOptions;
                                            System.out.println("[乱序] 题目" + question.getId() + " 使用已保存的选项顺序");
                                        } else {
                                            // 生成新的选项顺序
                                            Random optionRandom = new Random(System.nanoTime() ^ System.identityHashCode(questionData) ^ System.currentTimeMillis());
                                            Collections.shuffle(optionsEntries, optionRandom);
                                            System.out.println("[乱序] 题目" + question.getId() + " 选项已打乱");
                                            
                                            // 保存选项顺序
                                            if (optionOrders != null) {
                                                cn.hutool.json.JSONArray newOptionOrder = new cn.hutool.json.JSONArray();
                                                for (Map.Entry<String, String> entry : optionsEntries) {
                                                    newOptionOrder.add(entry.getKey());
                                                }
                                                optionOrders.set(String.valueOf(questionId), newOptionOrder);
                                            }
                                        }
                                    } catch (Exception e) {
                                        System.err.println("[乱序] 解析保存的选项顺序失败");
                                        e.printStackTrace();
                                    }
                                } else {
                                    // 为每道题的选项生成独立的随机种子，确保每题选项顺序都不同
                                    Random optionRandom = new Random(System.nanoTime() ^ System.identityHashCode(questionData) ^ System.currentTimeMillis());
                                    Collections.shuffle(optionsEntries, optionRandom);
                                    System.out.println("[乱序] 题目" + question.getId() + " 选项已打乱");
                                    
                                    // 保存选项顺序
                                    if (savedQuestionOrder != null) {
                                        try {
                                            cn.hutool.json.JSONObject orderObj = JSONUtil.parseObj(savedQuestionOrder);
                                            cn.hutool.json.JSONObject optionOrders = orderObj.getJSONObject("optionOrders");
                                            cn.hutool.json.JSONArray newOptionOrder = new cn.hutool.json.JSONArray();
                                            for (Map.Entry<String, String> entry : optionsEntries) {
                                                newOptionOrder.add(entry.getKey());
                                            }
                                            optionOrders.set(String.valueOf(questionId), newOptionOrder);
                                        } catch (Exception e) {
                                            System.err.println("[乱序] 保存选项顺序失败");
                                        }
                                    }
                                }
                            }

                            // 将乱序后的选项按顺序赋给 A, B, C, D
                            String[] keys = {"A", "B", "C", "D"};
                            for (int i = 0; i < Math.min(optionsEntries.size(), 4); i++) {
                                String optionValue = optionsEntries.get(i).getValue();
                                // 移除选项内容开头的标签（如"A. "、"B. "）
                                if (optionValue != null) {
                                    optionValue = optionValue.replaceFirst("^[A-D]\\.\\s*", "");
                                }
                                questionData.put("option" + keys[i], optionValue);
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

            resultMap.put("questions", result);
            resultMap.put("questionOrder", questionOrderJson);
            return resultMap;
        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("questions", new ArrayList<>());
            resultMap.put("questionOrder", null);
            return resultMap;
        }
    }

    /**
     * 获取生成的题目顺序JSON（用于保存到数据库）
     */
    public String getQuestionOrderJson() {
        // 这个方法不会被调用，我们通过修改返回类型来解决
        return null;
    }
}