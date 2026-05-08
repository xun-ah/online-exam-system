USE exam_system;

-- 修复学生2025005的密码
UPDATE sys_user SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi' WHERE username = '2025005';

SELECT username, password FROM sys_user WHERE username = '2025005';
