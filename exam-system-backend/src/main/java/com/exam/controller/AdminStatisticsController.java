package com.exam.controller;

import com.exam.common.Result;
import com.exam.mapper.DepartmentMapper;
import com.exam.mapper.QuestionMapper;
import com.exam.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/admin/statistics")
public class AdminStatisticsController {
    
    @Autowired
    private DashboardService dashboardService;
    
    @Autowired
    private DepartmentMapper departmentMapper;
    
    @Autowired
    private QuestionMapper questionMapper;
    
    /**
     * 获取仪表盘统计数据
     */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = dashboardService.getDashboardStats();
        return Result.success(stats);
    }
    
    /**
     * 获取各院系考试参与情况
     */
    @GetMapping("/department-exam")
    public Result<List<Map<String, Object>>> getDepartmentExamStats() {
        List<Map<String, Object>> data = departmentMapper.countExamParticipationByDepartment();
        return Result.success(data);
    }
    
    /**
     * 获取题型分布统计
     */
    @GetMapping("/question-type")
    public Result<List<Map<String, Object>>> getQuestionTypeStats() {
        List<Map<String, Object>> typeData = questionMapper.countByType();
        
        // 计算总数
        int total = typeData.stream()
            .mapToInt(item -> ((Number) item.get("count")).intValue())
            .sum();
        
        // 返回实际数量，前端自行计算百分比
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : typeData) {
            Map<String, Object> typeStat = new HashMap<>();
            typeStat.put("name", item.get("name"));
            int count = ((Number) item.get("count")).intValue();
            typeStat.put("count", count);
            result.add(typeStat);
        }
        
        return Result.success(result);
    }
    
    /**
     * 获取月度考试趋势
     */
    @GetMapping("/monthly-trend")
    public Result<List<Map<String, Object>>> getMonthlyTrend() {
        List<Map<String, Object>> data = questionMapper.countMonthlyTrend();
        return Result.success(data);
    }
}
