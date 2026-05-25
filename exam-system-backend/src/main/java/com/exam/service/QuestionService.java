package com.exam.service;

import com.exam.common.PageResult;
import com.exam.entity.Question;
import com.exam.mapper.QuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionService {
    
    @Autowired
    private QuestionMapper questionMapper;
    
    /**
     * 分页查询题目列表
     */
    public PageResult<Question> getQuestionList(Long teacherId, Integer type, Integer difficulty, 
                                                 String keyword, String subject, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Question> list = questionMapper.selectList(teacherId, type, difficulty, keyword, subject, offset, pageSize);
        int total = questionMapper.count(teacherId, type, difficulty, keyword, subject);
        return new PageResult<>((long) total, list);
    }
    
    /**
     * 根据ID查询题目
     */
    public Question getQuestionById(Long id) {
        return questionMapper.selectById(id);
    }
    
    /**
     * 新增题目
     */
    public void createQuestion(Question question) {
        questionMapper.insert(question);
    }
    
    /**
     * 更新题目
     */
    public void updateQuestion(Question question) {
        questionMapper.updateById(question);
    }
    
    /**
     * 删除题目
     */
    public void deleteQuestion(Long id) {
        questionMapper.deleteById(id);
    }
    
    /**
     * 统计题目数量
     */
    public int countQuestions(Long teacherId) {
        return questionMapper.countByTeacherId(teacherId);
    }
    
    /**
     * 根据条件获取所有题目（用于导出）
     */
    public List<Question> getQuestionsByCondition(Long teacherId, Integer type, Integer difficulty,
                                                  String keyword, String subject) {
        return questionMapper.selectListByCondition(teacherId, type, difficulty, keyword, subject);
    }
}
