package com.exam.service;

import com.exam.common.PageResult;
import com.exam.entity.Exam;
import com.exam.mapper.ExamMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExamService {
    
    @Autowired
    private ExamMapper examMapper;
    
    /**
     * 动态计算考试状态
     * 0-未开始 1-进行中 2-已结束
     */
    private void updateExamStatus(Exam exam) {
        if (exam == null || exam.getStartTime() == null || exam.getEndTime() == null) {
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(exam.getStartTime())) {
            exam.setStatus(0); // 未开始
        } else if (now.isAfter(exam.getEndTime())) {
            exam.setStatus(2); // 已结束
        } else {
            exam.setStatus(1); // 进行中
        }
    }
    
    /**
     * 获取考试列表
     */
    public PageResult<Exam> getExamList(Long teacherId, Long classId, Integer status, int pageNum, int pageSize) {
        List<Exam> allExams = examMapper.selectList(teacherId, classId, status);
        
        if (allExams == null) {
            allExams = new ArrayList<>();
        }
        
        // 动态计算每个考试的状态
        allExams.forEach(this::updateExamStatus);
        
        // 分页处理
        int total = allExams.size();
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, total);
        
        List<Exam> pageData = start < total 
            ? allExams.subList(start, end) 
            : new ArrayList<>();
        
        return new PageResult<>((long) total, pageData);
    }
    
    /**
     * 获取本院系的考试列表
     */
    public PageResult<Exam> getExamListByDepartment(Long departmentId, int pageNum, int pageSize) {
        List<Exam> allExams = examMapper.selectListByDepartmentId(departmentId);
        
        if (allExams == null) {
            allExams = new ArrayList<>();
        }
        
        // 动态计算每个考试的状态
        allExams.forEach(this::updateExamStatus);
        
        // 分页处理
        int total = allExams.size();
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, total);
        
        List<Exam> pageData = start < total 
            ? allExams.subList(start, end) 
            : new ArrayList<>();
        
        return new PageResult<>((long) total, pageData);
    }
    
    /**
     * 获取考试详情
     */
    public Exam getExamById(Long id) {
        Exam exam = examMapper.selectById(id);
        // 动态计算状态
        if (exam != null) {
            updateExamStatus(exam);
        }
        return exam;
    }
    
    /**
     * 创建考试
     */
    public void createExam(Exam exam, Long userId) {
        exam.setTeacherId(userId);
        exam.setStatus(0); // 默认未开始
        examMapper.insert(exam);
    }
    
    /**
     * 更新考试
     */
    public void updateExam(Exam exam) {
        examMapper.updateById(exam);
    }
    
    /**
     * 删除考试
     */
    public void deleteExam(Long id) {
        examMapper.deleteById(id);
    }
}
