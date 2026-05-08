package com.exam;

import cn.hutool.crypto.digest.BCrypt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;

@SpringBootTest
public class FixStudentPasswordTest {

    @Autowired
    private DataSource dataSource;

    @Test
    public void fixPassword() throws Exception {
        String rawPassword = "123456";
        String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
        
        System.out.println("原始密码: " + rawPassword);
        System.out.println("加密密码: " + hashedPassword);
        
        // 更新数据库
        String sql = "UPDATE sys_user SET password = ? WHERE username = '2025005'";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, hashedPassword);
            int rows = pstmt.executeUpdate();
            System.out.println("更新成功，影响行数: " + rows);
        }
    }
}
