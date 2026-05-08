package com.exam;

import cn.hutool.crypto.digest.BCrypt;

public class GeneratePassword {
    public static void main(String[] args) {
        String rawPassword = "123456";
        String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
        
        System.out.println("原始密码: " + rawPassword);
        System.out.println("加密后的密码: " + hashedPassword);
        System.out.println("\nSQL更新语句:");
        System.out.println("UPDATE sys_user SET password = '" + hashedPassword + "' WHERE username = '2025005';");
    }
}
