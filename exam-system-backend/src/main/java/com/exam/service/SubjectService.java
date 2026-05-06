package com.exam.service;

import com.exam.common.PageResult;
import com.exam.entity.Subject;
import com.exam.mapper.SubjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubjectService {
    
    @Autowired
    private SubjectMapper subjectMapper;
    
    /**
     * 获取指定院系的启用科目列表
     */
    public List<Subject> getActiveSubjects(Long departmentId) {
        return subjectMapper.selectActiveList(departmentId);
    }
    
    /**
     * 获取所有科目列表
     */
    public List<Subject> getAllSubjects() {
        return subjectMapper.selectAll();
    }
    
    /**
     * 管理员获取科目列表（分页）
     */
    public PageResult<Subject> getSubjectList(String name, Long departmentId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Subject> list = subjectMapper.selectList(name, departmentId, offset, pageSize);
        int total = subjectMapper.count(name, departmentId);
        return new PageResult<>((long) total, list);
    }
    
    /**
     * 根据ID获取科目
     */
    public Subject getSubjectById(Long id) {
        return subjectMapper.selectById(id);
    }
    
    /**
     * 新增科目
     */
    public void createSubject(Subject subject) {
        subject.setStatus(1);
        // 如果没有指定排序顺序，默认设为 99（排在最后）
        if (subject.getSortOrder() == null) {
            subject.setSortOrder(99);
        }
        subjectMapper.insert(subject);
    }
    
    /**
     * 更新科目
     */
    public void updateSubject(Subject subject) {
        subjectMapper.update(subject);
    }
    
    /**
     * 删除科目
     */
    public void deleteSubject(Long id) {
        subjectMapper.deleteById(id);
    }
}
