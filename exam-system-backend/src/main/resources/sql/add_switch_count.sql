-- 添加切屏次数字段到考试记录表
ALTER TABLE exam_record ADD COLUMN switch_count INT DEFAULT 0 COMMENT '切屏次数' AFTER extra_minutes;
