package com.exam;

import cn.hutool.crypto.digest.BCrypt;
import org.junit.jupiter.api.Test;

public class PasswordVerificationTest {
    
    @Test
    public void testPassword() {
        String rawPassword = "123456";
        String hashedPassword = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi";
        
        // 验证密码
        boolean matches = BCrypt.checkpw(rawPassword, hashedPassword);
        System.out.println("=== 密码验证测试 ===");
        System.out.println("原始密码: " + rawPassword);
        System.out.println("哈希密码: " + hashedPassword);
        System.out.println("验证结果: " + (matches ? "✓ 匹配" : "✗ 不匹配"));
        
        if (!matches) {
            System.out.println("\n生成新的密码哈希:");
            String newHash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
            System.out.println(newHash);
            System.out.println("\n请使用以下SQL更新数据库:");
            System.out.println("UPDATE sys_user SET password = '" + newHash + "' WHERE username = 'admin';");
        }
    }
}
