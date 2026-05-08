-- 修复teacher_id为null的试卷记录
-- 查看当前教师信息
SELECT id, teacher_no, real_name, department_id FROM teacher;

-- 将teacher_id为null的试卷更新为教师ID=1（张老师）
UPDATE paper SET teacher_id = 1 WHERE id = 7 AND teacher_id IS NULL;
UPDATE paper SET teacher_id = 1 WHERE id = 13 AND teacher_id IS NULL;

-- 验证更新结果
SELECT id, paper_name, paper_no, subject, teacher_id FROM paper ORDER BY id;
