package com.exam.service;

import com.exam.mapper.ExamMapper;
import com.exam.mapper.ExamRecordMapper;
import com.exam.mapper.StudentMapper;
import com.exam.mapper.TeacherMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardService {
    
    @Autowired
    private StudentMapper studentMapper;
    
    @Autowired
    private TeacherMapper teacherMapper;
    
    @Autowired
    private ExamMapper examMapper;
    
    @Autowired
    private ExamRecordMapper examRecordMapper;
    
    /**
     * 获取仪表盘统计数据
     */
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 获取当前年月
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();
        
        // 获取上月年月
        LocalDate lastMonth = now.minusMonths(1);
        int lastMonthYear = lastMonth.getYear();
        int lastMonthValue = lastMonth.getMonthValue();
        
        // 获取总数
        int totalStudents = studentMapper.count(null, null, null, null);
        int totalTeachers = teacherMapper.count(null, null, null);
        int monthlyExams = examMapper.countByMonth(currentYear, currentMonth);
        int totalParticipants = examRecordMapper.countParticipants();
        
        // 获取上月数据用于计算增长率
        int lastMonthStudents = studentMapper.countByMonth(lastMonthYear, lastMonthValue);
        int lastMonthTeachers = teacherMapper.countByMonth(lastMonthYear, lastMonthValue);
        int lastMonthExams = examMapper.countByMonth(lastMonthYear, lastMonthValue);
        int lastMonthParticipants = examRecordMapper.countByMonth(lastMonthYear, lastMonthValue);
        
        // 计算增长率
        stats.put("totalStudents", totalStudents);
        stats.put("totalTeachers", totalTeachers);
        stats.put("monthlyExams", monthlyExams);
        stats.put("totalParticipants", totalParticipants);
        
        stats.put("studentGrowth", calculateGrowth(lastMonthStudents, totalStudents));
        stats.put("teacherGrowth", calculateGrowth(lastMonthTeachers, totalTeachers));
        stats.put("examGrowth", calculateGrowth(lastMonthExams, monthlyExams));
        stats.put("participantGrowth", calculateGrowth(lastMonthParticipants, totalParticipants));
        
        return stats;
    }
    
    /**
     * 计算增长率
     */
    private String calculateGrowth(int lastMonth, int current) {
        if (lastMonth == 0) {
            return current > 0 ? "+100%" : "+0%";
        }
        double growth = ((double) (current - lastMonth) / lastMonth) * 100;
        return (growth >= 0 ? "+" : "") + String.format("%.0f", growth) + "%";
    }
}
