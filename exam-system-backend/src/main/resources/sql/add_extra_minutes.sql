-- 为考试记录表添加延长考试时间字段
-- 请执行此SQL语句
ALTER TABLE exam_record ADD COLUMN extra_minutes INT DEFAULT 0 COMMENT '延长考试时间（分钟）' AFTER status;
