package com.exam.service;

import com.exam.common.PageResult;
import com.exam.entity.Student;
import com.exam.entity.User;
import com.exam.mapper.StudentMapper;
import com.exam.mapper.UserMapper;
import cn.hutool.crypto.digest.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentService {
    
    @Autowired
    private StudentMapper studentMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    public PageResult<Student> getStudentList(String studentNo, String realName, Long classId, Long departmentId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Student> list = studentMapper.selectList(studentNo, realName, classId, departmentId, offset, pageSize);
        int total = studentMapper.count(studentNo, realName, classId, departmentId);
        return new PageResult<>((long) total, list);
    }
    
    @Transactional
    public void createStudent(Student student) {
        // 创建用户账号
        User user = new User();
        user.setUsername(student.getStudentNo());
        user.setPassword(BCrypt.hashpw("123456")); // 默认密码
        user.setRealName(student.getRealName());
        user.setRole(3); // 学生角色
        user.setPhone(student.getPhone());
        user.setEmail(student.getEmail());
        user.setStatus(1);
        userMapper.insert(user);
        
        student.setUserId(user.getId());
        studentMapper.insert(student);
    }
    
    @Transactional
    public void updateStudent(Student student) {
        studentMapper.updateById(student);
        
        // 同步更新用户信息
        User user = userMapper.selectById(student.getUserId());
        if (user != null) {
            user.setRealName(student.getRealName());
            user.setPhone(student.getPhone());
            user.setEmail(student.getEmail());
            userMapper.updateById(user);
        }
    }
    
    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentMapper.selectById(id);
        if (student != null) {
            studentMapper.deleteById(id);
            userMapper.deleteById(student.getUserId());
        }
    }
}
