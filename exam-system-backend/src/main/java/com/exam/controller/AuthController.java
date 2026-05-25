package com.exam.controller;

import com.exam.common.Result;
import com.exam.entity.User;
import com.exam.service.AuthService;
import com.exam.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/auth")
public class AuthController {
    
    // 使用内存存储验证码（key: username, value: captcha）
    private static final Map<String, String> captchaStore = new ConcurrentHashMap<>();
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 获取验证码
     */
    @GetMapping("/captcha")
    public Result<Map<String, String>> getCaptcha(@RequestParam(required = false) String username) {
        String captcha = generateCaptcha();
        // 将验证码存储到内存中，使用username作为key
        if (username != null && !username.isEmpty()) {
            captchaStore.put(username, captcha);
        }
        
        Map<String, String> data = new HashMap<>();
        data.put("captcha", captcha);
        
        return Result.success(data);
    }
    
    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginData, HttpSession session) {
        String username = loginData.get("username");
        String password = loginData.get("password");
        String captcha = loginData.get("captcha");
        String roleStr = loginData.get("role");
        
        // 调试日志
        System.out.println("=== 登录请求调试 ===");
        System.out.println("用户输入的验证码: " + captcha);
        String storedCaptcha = captchaStore.get(username);
        System.out.println("存储的验证码: " + storedCaptcha);
        System.out.println("===================");
        
        // 验证验证码
        if (storedCaptcha == null || !storedCaptcha.equalsIgnoreCase(captcha)) {
            return Result.error("验证码错误");
        }
        
        // 验证成功后删除验证码（防止重复使用）
        captchaStore.remove(username);
        
        // 验证账号密码
        User user = authService.login(username, password);
        if (user == null) {
            return Result.error("用户名或密码错误");
        }
        
        if (user.getStatus() == 0) {
            return Result.error("账号已被禁用");
        }
        
        // 校验前端选择的角色是否与账号实际角色一致
        if (roleStr != null && !roleStr.isEmpty()) {
            try {
                int selectedRole = Integer.parseInt(roleStr);
                if (user.getRole() != selectedRole) {
                    return Result.error("登录身份与账号不匹配，请选择正确的身份");
                }
            } catch (NumberFormatException e) {
                // 忽略格式错误，继续登录
            }
        }
        
        // 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("role", user.getRole());
        data.put("avatar", user.getAvatar()); // 添加头像字段
        
        return Result.success(data);
    }
    
    @GetMapping("/info")
    public Result<User> getUserInfo(@RequestAttribute("userId") Long userId) {
        User user = authService.getUserInfo(userId);
        if (user != null) {
            user.setPassword(null); // 不返回密码
        }
        return Result.success(user);
    }
    
    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Void> updatePassword(@RequestAttribute("userId") Long userId, 
                                       @RequestBody Map<String, String> passwordData) {
        String oldPassword = passwordData.get("oldPassword");
        String newPassword = passwordData.get("newPassword");
        
        if (oldPassword == null || newPassword == null) {
            return Result.error("参数不完整");
        }
        
        boolean success = authService.updatePassword(userId, oldPassword, newPassword);
        if (success) {
            return Result.success();
        } else {
            return Result.error("原密码错误");
        }
    }
    
    /**
     * 更新个人信息（手机号、邮箱）
     */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestAttribute("userId") Long userId,
                                      @RequestBody Map<String, String> profileData) {
        String phone = profileData.get("phone");
        String email = profileData.get("email");
        String realName = profileData.get("realName");
        
        boolean success = authService.updateProfile(userId, phone, email, realName);
        if (success) {
            return Result.success();
        } else {
            return Result.error("更新失败");
        }
    }
    
    /**
     * 更新头像
     */
    @PutMapping("/avatar")
    public Result<Map<String, String>> updateAvatar(@RequestAttribute("userId") Long userId,
                                                     @RequestBody Map<String, String> avatarData) {
        String avatarUrl = avatarData.get("avatar");
        
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            return Result.error("头像地址不能为空");
        }
        
        boolean success = authService.updateAvatar(userId, avatarUrl);
        if (success) {
            Map<String, String> data = new HashMap<>();
            data.put("avatar", avatarUrl);
            return Result.success(data);
        } else {
            return Result.error("头像更新失败");
        }
    }
    
    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        // 清除 session
        session.invalidate();
        return Result.success();
    }
    
    /**
     * 生成随机验证码
     */
    private String generateCaptcha() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random random = new Random();
        StringBuilder captcha = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            captcha.append(chars.charAt(random.nextInt(chars.length())));
        }
        return captcha.toString();
    }
    
    /**
     * 重置密码（无需登录）
     */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestBody Map<String, String> resetData) {
        String username = resetData.get("username");
        String phone = resetData.get("phone");
        String captcha = resetData.get("captcha");
        String newPassword = resetData.get("newPassword");
        
        // 验证验证码
        String storedCaptcha = captchaStore.get(username);
        if (storedCaptcha == null || !storedCaptcha.equalsIgnoreCase(captcha)) {
            return Result.error("验证码错误");
        }
        
        // 验证成功后删除验证码
        captchaStore.remove(username);
        
        // 重置密码
        boolean success = authService.resetPassword(username, phone, newPassword);
        if (success) {
            return Result.success();
        } else {
            return Result.error("重置失败，请检查账号和手机号");
        }
    }
}
