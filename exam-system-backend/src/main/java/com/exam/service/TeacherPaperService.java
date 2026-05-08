package com.exam.service;

import com.exam.common.PageResult;
import com.exam.entity.Paper;
import com.exam.entity.Teacher;
import com.exam.mapper.PaperMapper;
import com.exam.mapper.TeacherMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeacherPaperService {
    
    @Autowired
    private PaperMapper paperMapper;
    
    @Autowired
    private TeacherMapper teacherMapper;
    
    /**
     * 获取试卷列表（只返回本院系的试卷）
     */
    public PageResult<Paper> getPaperList(String paperName, Long userId, int pageNum, int pageSize) {
        // 获取教师所属院系ID
        Teacher teacher = teacherMapper.selectByUserId(userId);
        if (teacher == null) {
            return new PageResult<>(0L, new ArrayList<>());
        }
        Long departmentId = teacher.getDepartmentId();
        
        // 查询本院系的所有试卷（通过关联班级和教师的院系）
        List<Paper> allPapers = paperMapper.selectListByDepartmentId(departmentId);
        
        // 防止null
        if (allPapers == null) {
            allPapers = new ArrayList<>();
        }
        
        // 过滤未删除的试卷
        List<Paper> teacherPapers = allPapers.stream()
            .filter(p -> p.getDeleted() == null || p.getDeleted() == 0)
            .collect(Collectors.toList());
        
        // 如果指定了试卷名称,进行搜索过滤
        if (paperName != null && !paperName.isEmpty()) {
            teacherPapers = teacherPapers.stream()
                .filter(p -> p.getPaperName() != null && p.getPaperName().contains(paperName))
                .collect(Collectors.toList());
        }
        
        // 分页处理
        int total = teacherPapers.size();
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, total);
        
        List<Paper> pageData = start < total 
            ? teacherPapers.subList(start, end) 
            : new ArrayList<>();
        
        return new PageResult<>((long) total, pageData);
    }
    
    /**
     * 获取试卷详情
     */
    public Paper getPaperById(Long id) {
        return paperMapper.selectById(id);
    }
    
    /**
     * 创建试卷
     */
    public void createPaper(Paper paper, Long userId) {
        // 根据userId获取教师信息
        Teacher teacher = teacherMapper.selectByUserId(userId);
        if (teacher != null) {
            paper.setTeacherId(teacher.getId());
        }
        paperMapper.insert(paper);
    }
    
    /**
     * 更新试卷
     */
    public void updatePaper(Paper paper) {
        paperMapper.updateById(paper);
    }
    
    /**
     * 删除试卷
     */
    public void deletePaper(Long id) {
        paperMapper.deleteById(id);
    }
}
