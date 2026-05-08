-- 修复学生表中手机号和邮箱字段数据错位问题
-- 问题：phone字段存了院系名称，email字段存了手机号
-- 解决：使用临时变量交换两个字段的值

-- 查看当前错误数据（执行前可以先查看）
SELECT id, student_no, real_name, phone, email 
FROM student 
WHERE phone LIKE '%学院%' OR (email REGEXP '^[0-9]{11}$');

-- 修复数据：交换phone和email字段的值
UPDATE student 
SET phone = email, 
    email = phone 
WHERE phone LIKE '%学院%' OR (email REGEXP '^[0-9]{11}$' AND phone NOT REGEXP '^[0-9]{11}$');

-- 验证修复结果
SELECT id, student_no, real_name, phone, email 
FROM student 
ORDER BY id;
