-- 诊断学生考试列表为空的问题

-- 1. 查看学生李娜的信息和班级ID
SELECT id, student_no, real_name, class_id, department_id, user_id 
FROM student 
WHERE real_name = '李娜' OR student_no LIKE '%李娜%';

-- 2. 查看该班级是否有考试
SELECT e.id, e.exam_name, e.paper_id, e.class_id, e.start_time, e.end_time, e.status, e.deleted,
       p.paper_name, ci.class_name
FROM exam e
LEFT JOIN paper p ON e.paper_id = p.id
LEFT JOIN class_info ci ON e.class_id = ci.id
WHERE e.class_id = (SELECT class_id FROM student WHERE real_name = '李娜' LIMIT 1)
ORDER BY e.start_time DESC;

-- 3. 查看所有已发布的考试（按状态筛选）
SELECT id, exam_name, class_id, start_time, end_time, status, deleted
FROM exam
WHERE deleted = 0 OR deleted IS NULL
ORDER BY start_time DESC;

-- 4. 检查是否有考试记录（学生是否参加过考试）
SELECT er.id, er.exam_id, er.student_id, er.status, er.score,
       e.exam_name, s.real_name as student_name
FROM exam_record er
LEFT JOIN exam e ON er.exam_id = e.id
LEFT JOIN student s ON er.student_id = s.id
WHERE s.real_name = '李娜'
ORDER BY er.create_time DESC;

-- 5. 检查考试状态分布
SELECT status, COUNT(*) as count
FROM exam
WHERE deleted = 0 OR deleted IS NULL
GROUP BY status;

-- 6. 查看所有班级及其考试数量
SELECT ci.id, ci.class_name, 
       (SELECT COUNT(*) FROM exam WHERE class_id = ci.id AND (deleted = 0 OR deleted IS NULL)) as exam_count
FROM class_info ci
ORDER BY exam_count DESC;
