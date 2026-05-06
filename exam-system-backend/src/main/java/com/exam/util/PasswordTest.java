package com.exam.util;

import cn.hutool.crypto.digest.BCrypt;

public class PasswordTest {
    public static void main(String[] args) {
        String rawPassword = "123456";
        String hashedPassword = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi";
        
        // 验证密码
        boolean matches = BCrypt.checkpw(rawPassword, hashedPassword);
        System.out.println("密码验证结果: " + matches);
        
        // 如果不匹配,生成新的哈希
        if (!matches) {
            String newHash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
            System.out.println("新的密码哈希: " + newHash);
        }
        
        // 测试另一个哈希
        String dbHash = "$2a$10$.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHs";
        try {
            boolean dbMatches = BCrypt.checkpw(rawPassword, dbHash);
            System.out.println("数据库哈希验证结果: " + dbMatches);
        } catch (Exception e) {
            System.out.println("数据库哈希格式错误: " + e.getMessage());
        }
    }
}
