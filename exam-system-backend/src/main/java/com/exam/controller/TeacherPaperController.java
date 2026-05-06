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
            paper.setTeacherId(userId); // 简化处理
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
                List<Question> singles = getQuestionsByType(userId, subject, 1, ((Number)typeConfig.get("singleCount")).intValue());
                BigDecimal scorePerQ = new BigDecimal(typeConfig.getOrDefault("singleScore", 2).toString());
                for (Question q : singles) {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("questionId", q.getId());
                    detail.put("score", scorePerQ);
                    questionDetails.add(detail);
                    totalScore = totalScore.add(scorePerQ);
                }
            }

            // 多选题 (type=2)
            if (typeConfig.containsKey("multiCount") && ((Number)typeConfig.get("multiCount")).intValue() > 0) {
                List<Question> multis = getQuestionsByType(userId, subject, 2, ((Number)typeConfig.get("multiCount")).intValue());
                BigDecimal scorePerQ = new BigDecimal(typeConfig.getOrDefault("multiScore", 3).toString());
                for (Question q : multis) {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("questionId", q.getId());
                    detail.put("score", scorePerQ);
                    questionDetails.add(detail);
                    totalScore = totalScore.add(scorePerQ);
                }
            }

            // 判断题 (type=3)
            if (typeConfig.containsKey("judgeCount") && ((Number)typeConfig.get("judgeCount")).intValue() > 0) {
                List<Question> judges = getQuestionsByType(userId, subject, 3, ((Number)typeConfig.get("judgeCount")).intValue());
                BigDecimal scorePerQ = new BigDecimal(typeConfig.getOrDefault("judgeScore", 1).toString());
                for (Question q : judges) {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("questionId", q.getId());
                    detail.put("score", scorePerQ);
                    questionDetails.add(detail);
                    totalScore = totalScore.add(scorePerQ);
                }
            }

            // 填空题 (type=4)
            if (typeConfig.containsKey("fillCount") && ((Number)typeConfig.get("fillCount")).intValue() > 0) {
                List<Question> fills = getQuestionsByType(userId, subject, 4, ((Number)typeConfig.get("fillCount")).intValue());
                BigDecimal scorePerQ = new BigDecimal(typeConfig.getOrDefault("fillScore", 5).toString());
                for (Question q : fills) {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("questionId", q.getId());
                    detail.put("score", scorePerQ);
                    questionDetails.add(detail);
                    totalScore = totalScore.add(scorePerQ);
                }
            }

            // 简答题 (type=5)
            if (typeConfig.containsKey("essayCount") && ((Number)typeConfig.get("essayCount")).intValue() > 0) {
                List<Question> essays = getQuestionsByType(userId, subject, 5, ((Number)typeConfig.get("essayCount")).intValue());
                BigDecimal scorePerQ = new BigDecimal(typeConfig.getOrDefault("essayScore", 10).toString());
                for (Question q : essays) {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("questionId", q.getId());
                    detail.put("score", scorePerQ);
                    questionDetails.add(detail);
                    totalScore = totalScore.add(scorePerQ);
                }
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
        // 获取该题型所有题目
        List<Question> allQuestions = questionMapper.selectList(teacherId, type, null, null, subject, 0, 1000);
        
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
