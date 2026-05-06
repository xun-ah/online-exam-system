package com.exam.controller;

import com.exam.common.Result;
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
        List<Map<String, Object>> data = new ArrayList<>();
        data.add(createDeptStat("软件学院", 1200));
        data.add(createDeptStat("网络学院", 850));
        data.add(createDeptStat("人工智能", 650));
        data.add(createDeptStat("大数据", 720));
        data.add(createDeptStat("信息安全", 480));
        data.add(createDeptStat("计算机科学", 420));
        return Result.success(data);
    }
    
    /**
     * 获取题型分布统计
     */
    @GetMapping("/question-type")
    public Result<List<Map<String, Object>>> getQuestionTypeStats() {
        List<Map<String, Object>> data = new ArrayList<>();
        data.add(createTypeStat("单选题", 35));
        data.add(createTypeStat("多选题", 20));
        data.add(createTypeStat("判断题", 15));
        data.add(createTypeStat("填空题", 18));
        data.add(createTypeStat("简答题", 12));
        return Result.success(data);
    }
    
    /**
     * 获取月度考试趋势
     */
    @GetMapping("/monthly-trend")
    public Result<List<Map<String, Object>>> getMonthlyTrend() {
        List<Map<String, Object>> data = new ArrayList<>();
        String[] months = {"1月", "2月", "3月", "4月", "5月", "6月"};
        int[] values = {45, 38, 62, 78, 85, 92};
        for (int i = 0; i < months.length; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("month", months[i]);
            item.put("count", values[i]);
            data.add(item);
        }
        return Result.success(data);
    }
    
    private Map<String, Object> createDeptStat(String name, int count) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("count", count);
        return item;
    }
    
    private Map<String, Object> createTypeStat(String name, int percentage) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("percentage", percentage);
        return item;
    }
}
