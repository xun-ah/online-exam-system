package com.exam.controller;

import com.exam.common.Result;
import com.exam.entity.Paper;
import com.exam.entity.Question;
import com.exam.mapper.PaperMapper;
import com.exam.mapper.QuestionMapper;
import com.exam.service.TeacherPaperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/teacher/papers")
public class TeacherPaperController {

    @Autowired
    private TeacherPaperService paperService;

    @Autowired
    private PaperMapper paperMapper;

    @Autowired
    private QuestionMapper questionMapper;

    /**
     * 获取试卷列表
     */
    @GetMapping
    public Result<?> getPaperList(@RequestAttribute("userId") Long userId,
                                  @RequestParam(defaultValue = "1") int pageNum,
                                  @RequestParam(defaultValue = "10") int pageSize,
                                  @RequestParam(required = false) String paperName) {
        // 修正参数顺序以匹配Service定义: getPaperList(paperName, userId, pageNum, pageSize)
        return Result.success(paperService.getPaperList(paperName, userId, pageNum, pageSize));
    }

    /**
     * 获取试卷详情
     */
    @GetMapping("/{id}")
    public Result<Paper> getPaperDetail(@PathVariable Long id) {
        return Result.success(paperService.getPaperById(id));
    }

    /**
     * 获取试卷题目列表（用于预览）
     */
    @GetMapping("/{id}/questions")
    public Result<List<Map<String, Object>>> getPaperQuestions(@PathVariable Long id) {
        // 获取试卷信息
        Paper paper = paperMapper.selectById(id);
        if (paper == null) {
            return Result.error("试卷不存在");
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 解析试卷题目配置
        if (paper.getQuestionConfig() != null) {
            try {
                cn.hutool.json.JSONObject config = cn.hutool.json.JSONUtil.parseObj(paper.getQuestionConfig());
                cn.hutool.json.JSONArray questions = config.getJSONArray("questions");
                if (questions != null) {
                    for (int i = 0; i < questions.size(); i++) {
                        cn.hutool.json.JSONObject item = questions.getJSONObject(i);
                        Long questionId = item.getLong("questionId");
                        
                        // 获取题目详情
                        Question question = questionMapper.selectById(questionId);
                        if (question != null) {
                            Map<String, Object> questionData = new HashMap<>();
                            questionData.put("id", question.getId());
                            questionData.put("type", question.getType());
                            questionData.put("content", question.getContent());
                            
                            // 解析选项（JSON格式）
                            if (question.getOptions() != null && !question.getOptions().isEmpty()) {
                                try {
                                    cn.hutool.json.JSONObject opts = cn.hutool.json.JSONUtil.parseObj(question.getOptions());
                                    questionData.put("optionA", opts.getStr("A", ""));
                                    questionData.put("optionB", opts.getStr("B", ""));
                                    questionData.put("optionC", opts.getStr("C", ""));
                                    questionData.put("optionD", opts.getStr("D", ""));
                                } catch (Exception e) {
                                    questionData.put("optionA", "");
                                    questionData.put("optionB", "");
                                    questionData.put("optionC", "");
                                    questionData.put("optionD", "");
                                }
                            } else {
                                questionData.put("optionA", "");
                                questionData.put("optionB", "");
                                questionData.put("optionC", "");
                                questionData.put("optionD", "");
                            }
                            
                            questionData.put("answer", question.getAnswer());
                            questionData.put("analysis", question.getAnalysis());
                            questionData.put("difficulty", question.getDifficulty());
                            questionData.put("score", item.get("score")); // 使用试卷配置的分值
                            result.add(questionData);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return Result.success(result);
    }

    /**
     * 创建试卷
     */
    @PostMapping
    public Result<Void> createPaper(@RequestAttribute("userId") Long userId,
                                    @RequestBody Paper paper) {
        // 如果没有试卷编号，自动生成
        if (paper.getPaperNo() == null || paper.getPaperNo().isEmpty()) {
            paper.setPaperNo("P" + System.currentTimeMillis());
        }
        paperService.createPaper(paper, userId);
        return Result.success();
    }

    /**
     * 更新试卷
     */
    @PutMapping("/{id}")
    public Result<Void> updatePaper(@PathVariable Long id,
                                    @RequestBody Paper paper) {
        paper.setId(id);
        paperService.updatePaper(paper);
        return Result.success();
    }

    /**
     * 删除试卷
     */
    @DeleteMapping("/{id}")
    public Result<Void> deletePaper(@PathVariable Long id) {
        paperService.deletePaper(id);
        return Result.success();
    }

    /**
     * 智能组卷
     */
    @PostMapping("/auto-compose")
    public Result<Paper> autoCompose(@RequestAttribute("userId") Long userId,
                                     @RequestBody Map<String, Object> config) {
        try {
            String paperName = (String) config.get("paperName");
            String subject = (String) config.get("subject");
            Integer duration = (Integer) config.get("duration");
            Map<String, Object> typeConfig = (Map<String, Object>) config.get("typeConfig");

            // 构建试卷对象
            Paper paper = new Paper();
            paper.setPaperName(paperName);
            paper.setSubject(subject);
            paper.setDuration(duration);
            // teacherId会在service中设置
            paper.setStatus("unpublished");
            paper.setCreateTime(LocalDateTime.now());
            
            // 自动生成试卷编号
            String paperNo = "P" + System.currentTimeMillis();
            paper.setPaperNo(paperNo);

            // 随机抽取题目
            List<Map<String, Object>> questionDetails = new ArrayList<>();
            BigDecimal totalScore = BigDecimal.ZERO;

            // 单选题 (type=1)
            if (typeConfig.containsKey("singleCount") && ((Number)typeConfig.get("singleCount")).intValue() > 0) {
                int count = ((Number)typeConfig.get("singleCount")).intValue();
                List<Question> singles = getQuestionsByType(userId, subject, 1, count);
                BigDecimal scorePerQ = new BigDecimal(typeConfig.getOrDefault("singleScore", 2).toString());
                for (Question q : singles) {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("questionId", q.getId());
                    detail.put("score", scorePerQ);
                    questionDetails.add(detail);
                }
                // 按配置数量计算分数，而不是实际抽取数量
                totalScore = totalScore.add(scorePerQ.multiply(new BigDecimal(count)));
            }

            // 多选题 (type=2)
            if (typeConfig.containsKey("multiCount") && ((Number)typeConfig.get("multiCount")).intValue() > 0) {
                int count = ((Number)typeConfig.get("multiCount")).intValue();
                List<Question> multis = getQuestionsByType(userId, subject, 2, count);
                BigDecimal scorePerQ = new BigDecimal(typeConfig.getOrDefault("multiScore", 3).toString());
                for (Question q : multis) {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("questionId", q.getId());
                    detail.put("score", scorePerQ);
                    questionDetails.add(detail);
                }
                totalScore = totalScore.add(scorePerQ.multiply(new BigDecimal(count)));
            }

            // 判断题 (type=3)
            if (typeConfig.containsKey("judgeCount") && ((Number)typeConfig.get("judgeCount")).intValue() > 0) {
                int count = ((Number)typeConfig.get("judgeCount")).intValue();
                List<Question> judges = getQuestionsByType(userId, subject, 3, count);
                BigDecimal scorePerQ = new BigDecimal(typeConfig.getOrDefault("judgeScore", 1).toString());
                for (Question q : judges) {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("questionId", q.getId());
                    detail.put("score", scorePerQ);
                    questionDetails.add(detail);
                }
                totalScore = totalScore.add(scorePerQ.multiply(new BigDecimal(count)));
            }

            // 填空题 (type=4)
            if (typeConfig.containsKey("fillCount") && ((Number)typeConfig.get("fillCount")).intValue() > 0) {
                int count = ((Number)typeConfig.get("fillCount")).intValue();
                List<Question> fills = getQuestionsByType(userId, subject, 4, count);
                BigDecimal scorePerQ = new BigDecimal(typeConfig.getOrDefault("fillScore", 5).toString());
                for (Question q : fills) {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("questionId", q.getId());
                    detail.put("score", scorePerQ);
                    questionDetails.add(detail);
                }
                totalScore = totalScore.add(scorePerQ.multiply(new BigDecimal(count)));
            }

            // 简答题 (type=5)
            if (typeConfig.containsKey("essayCount") && ((Number)typeConfig.get("essayCount")).intValue() > 0) {
                int count = ((Number)typeConfig.get("essayCount")).intValue();
                List<Question> essays = getQuestionsByType(userId, subject, 5, count);
                BigDecimal scorePerQ = new BigDecimal(typeConfig.getOrDefault("essayScore", 10).toString());
                for (Question q : essays) {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("questionId", q.getId());
                    detail.put("score", scorePerQ);
                    questionDetails.add(detail);
                }
                totalScore = totalScore.add(scorePerQ.multiply(new BigDecimal(count)));
            }

            // 编程题 (type=6)
            if (typeConfig.containsKey("programCount") && ((Number)typeConfig.get("programCount")).intValue() > 0) {
                int count = ((Number)typeConfig.get("programCount")).intValue();
                List<Question> programs = getQuestionsByType(userId, subject, 6, count);
                BigDecimal scorePerQ = new BigDecimal(typeConfig.getOrDefault("programScore", 15).toString());
                for (Question q : programs) {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("questionId", q.getId());
                    detail.put("score", scorePerQ);
                    questionDetails.add(detail);
                }
                totalScore = totalScore.add(scorePerQ.multiply(new BigDecimal(count)));
            }

            paper.setTotalScore(totalScore);
            
            // 计算平均难度
            int totalDifficulty = 0;
            int questionCount = 0;
            for (Map<String, Object> detail : questionDetails) {
                Long qId = ((Number)detail.get("questionId")).longValue();
                Question q = questionMapper.selectById(qId);
                if (q != null && q.getDifficulty() != null) {
                    totalDifficulty += q.getDifficulty();
                    questionCount++;
                }
            }
            int avgDifficulty = questionCount > 0 ? Math.round((float)totalDifficulty / questionCount) : 2;
            paper.setDifficulty(avgDifficulty);
            
            // 包装成 questions 数组格式，与手动组卷保持一致
            Map<String, Object> configMap = new HashMap<>();
            configMap.put("questions", questionDetails);
            paper.setQuestionConfig(JSONUtil.toJsonStr(configMap));

            paperService.createPaper(paper, userId);
            return Result.success(paper);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("智能组卷失败: " + e.getMessage());
        }
    }

    /**
     * 根据题型随机获取题目
     */
    private List<Question> getQuestionsByType(Long teacherId, String subject, int type, int count) {
        // 只按科目和题型查询，不按教师过滤（题库共享）
        List<Question> allQuestions = questionMapper.selectList(null, type, null, null, subject, 0, 1000);
        
        // 随机打乱
        Collections.shuffle(allQuestions);
        
        // 取前 count 个
        return allQuestions.stream().limit(count).collect(Collectors.toList());
    }

    /**
     * 简单的 JSON 工具类（避免引入额外依赖）
     */
    static class JSONUtil {
        public static String toJsonStr(Object obj) {
            if (obj == null) return "null";
            if (obj instanceof Map) {
                return toMapJson((Map<?, ?>) obj);
            }
            if (obj instanceof List) {
                List<?> list = (List<?>) obj;
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    sb.append(toJsonStr(item));
                    if (i < list.size() - 1) sb.append(",");
                }
                sb.append("]");
                return sb.toString();
            }
            // 处理基本类型
            if (obj instanceof String) {
                return "\"" + obj + "\"";
            }
            return obj.toString();
        }

        private static String toMapJson(Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{");
            int i = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sb.append("\"" + entry.getKey() + "\":");
                sb.append(toJsonStr(entry.getValue()));
                if (i < map.size() - 1) sb.append(",");
                i++;
            }
            sb.append("}");
            return sb.toString();
        }
    }
}
