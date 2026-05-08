-- 1. 查看教师信息
SELECT id, teacher_no, real_name, department_id, user_id FROM teacher;

-- 2. 查看教师的班级关联
SELECT tc.id, tc.teacher_id, tc.class_id, tc.subject, c.class_name 
FROM teacher_class tc
LEFT JOIN class_info c ON tc.class_id = c.id;

-- 3. 查看所有学生
SELECT id, student_no, real_name, class_id, user_id FROM student;

-- 4. 查看所有用户
SELECT id, username, real_name, role FROM sys_user;

-- 5. 查看所有考试
SELECT id, exam_name, paper_id, class_id, start_time, end_time, status FROM exam;
