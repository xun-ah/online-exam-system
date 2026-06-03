package com.exam.controller;

import com.exam.annotation.SysLog;
import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.entity.Paper;
import com.exam.entity.Question;
import com.exam.entity.Teacher;
import com.exam.mapper.PaperMapper;
import com.exam.mapper.QuestionMapper;
import com.exam.mapper.TeacherMapper;
import com.exam.service.QuestionImportService;
import com.exam.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/teacher/questions")
public class QuestionController {
    
    @Autowired
    private QuestionService questionService;
    
    @Autowired
    private QuestionImportService questionImportService;
    
    @Autowired
    private TeacherMapper teacherMapper;
    
    @Autowired
    private PaperMapper paperMapper;
    
    @Autowired
    private QuestionMapper questionMapper;
    
    /**
     * 分页查询题目列表
     */
    @GetMapping
    public Result<PageResult<Question>> getQuestionList(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) boolean includeAll,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        
        Long teacherId = null;
        // 如果不包含所有题目，则按教师ID过滤
        if (!includeAll) {
            teacherId = getTeacherIdByUserId(userId);
        }
        
        PageResult<Question> result = questionService.getQuestionList(
            teacherId, type, difficulty, keyword, subject, pageNum, pageSize);
        return Result.success(result);
    }
    
    /**
     * 根据ID查询题目详情
     */
    @GetMapping("/{id}")
    public Result<Question> getQuestionById(@PathVariable Long id) {
        Question question = questionService.getQuestionById(id);
        return Result.success(question);
    }
    
    /**
     * 新增题目
     */
    @SysLog("新增题目")
    @PostMapping
    public Result<Void> createQuestion(@RequestAttribute("userId") Long userId,
                                       @RequestBody Question question) {
        // 设置教师ID
        Long teacherId = getTeacherIdByUserId(userId);
        question.setTeacherId(teacherId);
        
        // 自动生成题目编号（如果前端没有传）
        if (question.getQuestionNo() == null || question.getQuestionNo().isEmpty()) {
            String questionNo = generateQuestionNo(teacherId);
            question.setQuestionNo(questionNo);
        }
        
        questionService.createQuestion(question);
        return Result.success();
    }
    
    /**
     * 更新题目
     */
    @SysLog("更新题目")
    @PutMapping("/{id}")
    public Result<Void> updateQuestion(@PathVariable Long id,
                                       @RequestBody Question question) {
        question.setId(id);
        questionService.updateQuestion(question);
        return Result.success();
    }
    
    /**
     * 删除题目
     */
    @SysLog("删除题目")
    @DeleteMapping("/{id}")
    public Result<Void> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return Result.success();
    }
    
    /**
     * 批量删除题目
     * 注意：使用 POST 而不是 DELETE，因为 DELETE 请求体支持在不同容器中不一致，POST 更稳定
     */
    @SysLog("批量删除题目")
    @PostMapping("/batch-delete")
    public Result<Void> batchDeleteQuestions(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的题目");
        }
        
        for (Long id : ids) {
            questionService.deleteQuestion(id);
        }
        
        return Result.success();
    }
    
    /**
     * 获取题目统计信息
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(@RequestAttribute("userId") Long userId) {
        Long teacherId = getTeacherIdByUserId(userId);
        int total = questionService.countQuestions(teacherId);
        
        // 本月新增
        int monthQuestions = questionMapper.countByTeacherIdAndMonth(teacherId);
        
        // 覆盖科目
        int subjectCount = questionMapper.countDistinctSubjectByTeacherId(teacherId);
        
        // 被引用次数：遍历该教师的所有试卷，统计题目ID属于该教师的出现次数
        int usageCount = 0;
        List<Paper> papers = paperMapper.selectList(teacherId);
        if (papers != null) {
            for (Paper paper : papers) {
                if (paper.getQuestionConfig() != null) {
                    try {
                        cn.hutool.json.JSONObject config = cn.hutool.json.JSONUtil.parseObj(paper.getQuestionConfig());
                        cn.hutool.json.JSONArray questions = config.getJSONArray("questions");
                        if (questions != null) {
                            for (int i = 0; i < questions.size(); i++) {
                                cn.hutool.json.JSONObject q = questions.getJSONObject(i);
                                Long qId = q.getLong("questionId");
                                if (qId != null) {
                                    Question qDetail = questionMapper.selectById(qId);
                                    if (qDetail != null && qDetail.getTeacherId().equals(teacherId)) {
                                        usageCount++;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        // 忽略解析错误
                    }
                }
            }
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalQuestions", total);
        stats.put("monthQuestions", monthQuestions);
        stats.put("subjectCount", subjectCount);
        stats.put("usageCount", usageCount);
        
        return Result.success(stats);
    }
    
    /**
     * 批量导入题目（支持Word/Excel）
     */
    @SysLog("批量导入题目")
    @PostMapping("/batch-import")
    public Result<Map<String, Object>> batchImportQuestions(
            @RequestAttribute("userId") Long userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "subject", required = false) String subject) {
        try {
            // 验证文件
            if (file == null || file.isEmpty()) {
                return Result.error("请上传文件");
            }
            
            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.endsWith(".docx") && !fileName.endsWith(".xlsx"))) {
                return Result.error("仅支持.docx和.xlsx格式的文件");
            }
            
            Long teacherId = getTeacherIdByUserId(userId);
            int successCount = questionImportService.importQuestions(file, teacherId, subject);
            
            Map<String, Object> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("message", "成功导入" + successCount + "道题目");
            
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("导入失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量导出题目
     */
    @GetMapping("/batch-export")
    public void batchExportQuestions(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String subject,
            HttpServletResponse response) {
        try {
            Long teacherId = getTeacherIdByUserId(userId);
            questionImportService.exportQuestions(teacherId, type, difficulty, keyword, subject, response);
        } catch (Exception e) {
            e.printStackTrace();
            // 返回错误信息
            response.setStatus(500);
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"code\":500,\"message\":\"导出失败: " + e.getMessage() + "\"}");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    /**
     * 根据userId获取teacherId
     */
    private Long getTeacherIdByUserId(Long userId) {
        Teacher teacher = teacherMapper.selectByUserId(userId);
        if (teacher != null) {
            return teacher.getId();
        }
        return null;
    }
    
    /**
     * 生成题目编号
     * 格式：Q + 教师ID + 时间戳后6位
     */
    private String generateQuestionNo(Long teacherId) {
        long timestamp = System.currentTimeMillis();
        String timestampSuffix = String.valueOf(timestamp).substring(7); // 取后6位
        return "Q" + teacherId + timestampSuffix;
    }
}
