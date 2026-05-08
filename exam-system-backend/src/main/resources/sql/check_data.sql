-- 查看教师表数据
SELECT id, teacher_no, real_name, department_id, user_id FROM teacher;

-- 查看试卷表中teacher_id为null的记录
SELECT id, paper_name, paper_no, subject, teacher_id FROM paper WHERE teacher_id IS NULL;
