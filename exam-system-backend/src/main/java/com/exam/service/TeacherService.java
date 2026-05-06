package com.exam.service;

import com.exam.common.PageResult;
import com.exam.entity.Teacher;
import com.exam.entity.User;
import com.exam.mapper.TeacherMapper;
import com.exam.mapper.UserMapper;
import cn.hutool.crypto.digest.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeacherService {
    
    @Autowired
    private TeacherMapper teacherMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    public PageResult<Teacher> getTeacherList(String teacherNo, String realName, Long departmentId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Teacher> list = teacherMapper.selectList(teacherNo, realName, departmentId, offset, pageSize);
        int total = teacherMapper.count(teacherNo, realName, departmentId);
        return new PageResult<>((long) total, list);
    }
    
    @Transactional
    public void createTeacher(Teacher teacher) {
        // 检查工号是否已存在
        Teacher existingTeacher = teacherMapper.selectByTeacherNo(teacher.getTeacherNo());
        if (existingTeacher != null) {
            throw new RuntimeException("工号已存在：" + teacher.getTeacherNo());
        }
        
        // 检查用户名是否已存在
        User existingUser = userMapper.selectByUsername(teacher.getTeacherNo());
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在：" + teacher.getTeacherNo());
        }
        
        // 创建用户账号
        User user = new User();
        user.setUsername(teacher.getTeacherNo());
        user.setPassword(BCrypt.hashpw("123456")); // 默认密码
        user.setRealName(teacher.getRealName());
        user.setRole(2); // 教师角色
        user.setPhone(teacher.getPhone());
        user.setEmail(teacher.getEmail());
        user.setStatus(1);
        userMapper.insert(user);
        
        teacher.setUserId(user.getId());
        teacherMapper.insert(teacher);
    }
    
    @Transactional
    public void updateTeacher(Teacher teacher) {
        teacherMapper.updateById(teacher);
        
        // 同步更新用户信息
        User user = userMapper.selectById(teacher.getUserId());
        if (user != null) {
            user.setRealName(teacher.getRealName());
            user.setPhone(teacher.getPhone());
            user.setEmail(teacher.getEmail());
            userMapper.updateById(user);
        }
    }
    
    @Transactional
    public void deleteTeacher(Long id) {
        Teacher teacher = teacherMapper.selectById(id);
        if (teacher != null) {
            teacherMapper.deleteById(id);
            userMapper.deleteById(teacher.getUserId());
        }
    }
}
