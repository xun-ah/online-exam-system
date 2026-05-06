package com.exam.service;

import com.exam.entity.Teacher;
import com.exam.entity.User;
import com.exam.mapper.TeacherMapper;
import com.exam.mapper.UserMapper;
import cn.hutool.crypto.digest.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private TeacherMapper teacherMapper;
    
    public User login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            return null;
        }
        
        // 验证密码（BCrypt加密）
        if (BCrypt.checkpw(password, user.getPassword())) {
            return user;
        }
        
        return null;
    }
    
    public User getUserInfo(Long userId) {
        return userMapper.selectById(userId);
    }
    
    /**
     * 修改密码
     */
    public boolean updatePassword(Long userId, String oldPassword, String newPassword) {
        // 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        
        // 验证旧密码
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            return false;
        }
        
        // 加密新密码
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        
        // 更新密码
        user.setPassword(hashedPassword);
        int rows = userMapper.updateById(user);
        
        return rows > 0;
    }
    
    /**
     * 更新个人信息（同时更新sys_user表和teacher表）
     */
    @Transactional
    public boolean updateProfile(Long userId, String phone, String email, String realName) {
        // 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        
        // 更新sys_user表
        if (phone != null && !phone.isEmpty()) {
            user.setPhone(phone);
        }
        if (email != null && !email.isEmpty()) {
            user.setEmail(email);
        }
        if (realName != null && !realName.isEmpty()) {
            user.setRealName(realName);
        }
        
        int userRows = userMapper.updateById(user);
        
        // 同步更新teacher表
        Teacher teacher = teacherMapper.selectByUserId(userId);
        if (teacher != null) {
            if (realName != null && !realName.isEmpty()) {
                teacher.setRealName(realName);
            }
            if (phone != null && !phone.isEmpty()) {
                teacher.setPhone(phone);
            }
            if (email != null && !email.isEmpty()) {
                teacher.setEmail(email);
            }
            teacherMapper.updateById(teacher);
        }
        
        return userRows > 0;
    }
    
    /**
     * 更新头像（同时更新sys_user表和teacher表）
     */
    @Transactional
    public boolean updateAvatar(Long userId, String avatarUrl) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        
        // 更新sys_user表
        user.setAvatar(avatarUrl);
        int userRows = userMapper.updateById(user);
        
        // 同步更新teacher表（如果需要）
        Teacher teacher = teacherMapper.selectByUserId(userId);
        if (teacher != null) {
            // 如果teacher表有avatar字段，也同步更新
            // 目前teacher表可能没有avatar字段，这里先留空
        }
        
        return userRows > 0;
    }
}
