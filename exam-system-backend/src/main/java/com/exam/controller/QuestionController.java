package com.exam.controller;

import com.exam.annotation.SysLog;
import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.entity.Question;
import com.exam.entity.Teacher;
import com.exam.mapper.TeacherMapper;
import com.exam.service.QuestionImportService;
import com.exam.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    
    /**
     * 分页查询题目列表
     */
    @SysLog("查询题目列表")
    @GetMapping
    public Result<PageResult<Question>> getQuestionList(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String subject,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        
        // 获取教师ID（这里简化处理，实际应该根据userId查询teacher表）
        Long teacherId = getTeacherIdByUserId(userId);
        
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
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalQuestions", total);
        stats.put("monthQuestions", 0); // TODO: 实现本月新增统计
        stats.put("subjectCount", 0);   // TODO: 实现科目数量统计
        stats.put("usageCount", 0);     // TODO: 实现引用次数统计
        
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
            int successCount = questionImportService.importQuestionsFromWord(file, teacherId, subject);
            
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
