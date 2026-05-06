-- 创建数据库
CREATE DATABASE IF NOT EXISTS exam_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE exam_system;

-- ==================== 表结构定义 ====================

-- 用户表
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码',
  `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
  `role` INT NOT NULL COMMENT '角色：1-管理员 2-教师 3-学生',
  `phone` VARCHAR(20) COMMENT '手机号',
  `email` VARCHAR(100) COMMENT '邮箱',
  `avatar` VARCHAR(255) COMMENT '头像URL',
  `status` INT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 院系列表
CREATE TABLE `department` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '院系ID',
  `dept_name` VARCHAR(100) NOT NULL COMMENT '院系名称',
  `dept_code` VARCHAR(50) COMMENT '院系代码',
  `description` VARCHAR(500) COMMENT '描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='院系列表';

-- 班级表
CREATE TABLE `class_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '班级ID',
  `class_name` VARCHAR(100) NOT NULL COMMENT '班级名称',
  `class_code` VARCHAR(50) COMMENT '班级代码',
  `department_id` BIGINT COMMENT '院系ID',
  `grade` VARCHAR(20) COMMENT '年级',
  `description` VARCHAR(500) COMMENT '描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

-- 学生表
CREATE TABLE `student` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '学生ID',
  `student_no` VARCHAR(50) NOT NULL COMMENT '学号',
  `real_name` VARCHAR(50) NOT NULL COMMENT '姓名',
  `gender` INT COMMENT '性别：0-女 1-男',
  `phone` VARCHAR(20) COMMENT '手机号',
  `email` VARCHAR(100) COMMENT '邮箱',
  `class_id` BIGINT COMMENT '班级ID',
  `department_id` BIGINT COMMENT '院系ID',
  `user_id` BIGINT COMMENT '关联用户ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_no` (`student_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生表';

-- 教师表
CREATE TABLE `teacher` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '教师ID',
  `teacher_no` VARCHAR(50) NOT NULL COMMENT '工号',
  `real_name` VARCHAR(50) NOT NULL COMMENT '姓名',
  `gender` INT COMMENT '性别：0-女 1-男',
  `phone` VARCHAR(20) COMMENT '手机号',
  `email` VARCHAR(100) COMMENT '邮箱',
  `department_id` BIGINT COMMENT '院系ID',
  `user_id` BIGINT COMMENT '关联用户ID',
  `status` INT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teacher_no` (`teacher_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师表';

-- 题库表
CREATE TABLE `question` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '题目ID',
  `question_no` VARCHAR(50) NOT NULL COMMENT '题目编号',
  `type` INT NOT NULL COMMENT '类型：1-单选题 2-多选题 3-判断题 4-填空题 5-简答题',
  `content` TEXT NOT NULL COMMENT '题目内容',
  `options` TEXT COMMENT '选项（JSON格式）',
  `answer` TEXT NOT NULL COMMENT '正确答案',
  `analysis` TEXT COMMENT '答案解析',
  `score` DECIMAL(5,2) DEFAULT 0 COMMENT '分值',
  `teacher_id` BIGINT COMMENT '出题教师ID',
  `subject` VARCHAR(50) COMMENT '所属科目',
  `knowledge_point` VARCHAR(200) COMMENT '知识点',
  `difficulty` INT DEFAULT 2 COMMENT '难度：1-简单 2-中等 3-困难',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_question_no` (`question_no`),
  KEY `idx_subject` (`subject`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库表';

-- 科目表
CREATE TABLE IF NOT EXISTS `subject` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `department_id` BIGINT DEFAULT NULL COMMENT '所属院系ID',
  `name` VARCHAR(50) NOT NULL COMMENT '科目名称',
  `code` VARCHAR(20) NOT NULL COMMENT '科目代码',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
  `credits` INT DEFAULT 3 COMMENT '学分',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_department_id` (`department_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科目表';

-- 试卷表
CREATE TABLE `paper` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '试卷ID',
  `paper_name` VARCHAR(200) NOT NULL COMMENT '试卷名称',
  `paper_no` VARCHAR(50) NOT NULL COMMENT '试卷编号',
  `total_score` DECIMAL(6,2) DEFAULT 0 COMMENT '总分',
  `duration` INT COMMENT '考试时长（分钟）',
  `teacher_id` BIGINT COMMENT '创建教师ID',
  `description` VARCHAR(500) COMMENT '描述',
  `subject` VARCHAR(100) COMMENT '科目',
  `difficulty` INT DEFAULT 2 COMMENT '难度：1-易 2-中 3-难',
  `status` VARCHAR(20) DEFAULT 'unpublished' COMMENT '状态：published-已发布 unpublished-未发布 ended-已结束',
  `question_config` TEXT COMMENT '题目配置（JSON格式）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_paper_no` (`paper_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷表';

-- 试卷题目关联表
CREATE TABLE `paper_question` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `paper_id` BIGINT NOT NULL COMMENT '试卷ID',
  `question_id` BIGINT NOT NULL COMMENT '题目ID',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `score` DECIMAL(5,2) DEFAULT 0 COMMENT '本题分值',
  PRIMARY KEY (`id`),
  KEY `idx_paper_id` (`paper_id`),
  KEY `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷题目关联表';

-- 考试表
CREATE TABLE `exam` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '考试ID',
  `exam_name` VARCHAR(200) NOT NULL COMMENT '考试名称',
  `paper_id` BIGINT NOT NULL COMMENT '试卷ID',
  `teacher_id` BIGINT COMMENT '发布教师ID',
  `start_time` DATETIME COMMENT '开始时间',
  `end_time` DATETIME COMMENT '结束时间',
  `duration` INT COMMENT '考试时长（分钟）',
  `class_id` BIGINT COMMENT '考试班级ID',
  `status` INT DEFAULT 0 COMMENT '状态：0-未开始 1-进行中 2-已结束',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试表';

-- 考试记录表
CREATE TABLE `exam_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `exam_id` BIGINT NOT NULL COMMENT '考试ID',
  `student_id` BIGINT NOT NULL COMMENT '学生ID',
  `score` DECIMAL(6,2) DEFAULT 0 COMMENT '得分',
  `answers` TEXT COMMENT '学生答案（JSON格式）',
  `submit_time` DATETIME COMMENT '提交时间',
  `status` INT DEFAULT 0 COMMENT '状态：0-考试中 1-已提交 2-已阅卷',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_exam_id` (`exam_id`),
  KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试记录表';

-- 错题本表
CREATE TABLE `wrong_book` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `student_id` BIGINT NOT NULL COMMENT '学生ID',
  `question_id` BIGINT NOT NULL COMMENT '题目ID',
  `exam_id` BIGINT COMMENT '考试ID',
  `wrong_answer` TEXT COMMENT '错误答案',
  `correct_answer` TEXT COMMENT '正确答案',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本表';

-- 教师班级关联表
CREATE TABLE `teacher_class` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `teacher_id` BIGINT NOT NULL COMMENT '教师ID',
  `class_id` BIGINT NOT NULL COMMENT '班级ID',
  `subject` VARCHAR(100) COMMENT '任教科目',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teacher_class` (`teacher_id`, `class_id`),
  KEY `idx_class_id` (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师班级关联表';

-- 系统日志表
CREATE TABLE `system_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` BIGINT COMMENT '用户ID',
  `username` VARCHAR(50) COMMENT '用户名',
  `operation` VARCHAR(200) COMMENT '操作内容',
  `method` VARCHAR(200) COMMENT '请求方法',
  `params` TEXT COMMENT '请求参数',
  `ip` VARCHAR(50) COMMENT 'IP地址',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统日志表';

-- 插入初始数据

-- 管理员用户
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `phone`, `email`, `status`) 
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 1, '13800138000', 'admin@exam.com', 1);

-- 测试教师用户
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `phone`, `email`, `status`) 
VALUES ('teacher1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张老师', 2, '13800138001', 'teacher1@exam.com', 1);

-- 测试学生用户
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `phone`, `email`, `status`) 
VALUES ('student1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李四', 3, '13800138002', 'student1@exam.com', 1);

-- 院系数据
INSERT INTO `department` (`dept_name`, `dept_code`, `description`) VALUES 
('计算机学院', 'CS', '计算机科学与技术学院'),
('数学学院', 'MATH', '数学与统计学院'),
('外国语学院', 'FL', '外国语学院'),
('汉语言学院', 'CHIN', '汉语言学院'),
('经济管理学院', 'JJGL', '经济管理学院'),
('法学院', 'LAW', '法学院'),
('电子信息工程学院', 'EI', '电子信息工程学院');

-- 班级数据（每个院系2个年级，每个年级2个班级，共28个班级）
INSERT INTO `class_info` (`class_name`, `class_code`, `department_id`, `grade`, `description`) VALUES 
-- 计算机学院
('计算机2021级1班', 'CS202101', 1, '2021', '计算机学院2021级1班'),
('计算机2021级2班', 'CS202102', 1, '2021', '计算机学院2021级2班'),
('计算机2022级1班', 'CS202201', 1, '2022', '计算机学院2022级1班'),
('计算机2022级2班', 'CS202202', 1, '2022', '计算机学院2022级2班'),
-- 数学学院
('数学2021级1班', 'MATH202101', 2, '2021', '数学学院2021级1班'),
('数学2021级2班', 'MATH202102', 2, '2021', '数学学院2021级2班'),
('数学2022级1班', 'MATH202201', 2, '2022', '数学学院2022级1班'),
('数学2022级2班', 'MATH202202', 2, '2022', '数学学院2022级2班'),
-- 外国语学院
('外语2021级1班', 'FL202101', 3, '2021', '外国语学院2021级1班'),
('外语2021级2班', 'FL202102', 3, '2021', '外国语学院2021级2班'),
('外语2022级1班', 'FL202201', 3, '2022', '外国语学院2022级1班'),
('外语2022级2班', 'FL202202', 3, '2022', '外国语学院2022级2班'),
-- 汉语言学院
('汉语言2021级1班', 'CHIN202101', 4, '2021', '汉语言学院2021级1班'),
('汉语言2021级2班', 'CHIN202102', 4, '2021', '汉语言学院2021级2班'),
('汉语言2022级1班', 'CHIN202201', 4, '2022', '汉语言学院2022级1班'),
('汉语言2022级2班', 'CHIN202202', 4, '2022', '汉语言学院2022级2班'),
-- 经济管理学院
('经管2021级1班', 'JJGL202101', 5, '2021', '经济管理学院2021级1班'),
('经管2021级2班', 'JJGL202102', 5, '2021', '经济管理学院2021级2班'),
('经管2022级1班', 'JJGL202201', 5, '2022', '经济管理学院2022级1班'),
('经管2022级2班', 'JJGL202202', 5, '2022', '经济管理学院2022级2班'),
-- 法学院
('法学2021级1班', 'LAW202101', 6, '2021', '法学院2021级1班'),
('法学2021级2班', 'LAW202102', 6, '2021', '法学院2021级2班'),
('法学2022级1班', 'LAW202201', 6, '2022', '法学院2022级1班'),
('法学2022级2班', 'LAW202202', 6, '2022', '法学院2022级2班'),
-- 电子信息工程学院
('电子2021级1班', 'EI202101', 7, '2021', '电子信息工程学院2021级1班'),
('电子2021级2班', 'EI202102', 7, '2021', '电子信息工程学院2021级2班'),
('电子2022级1班', 'EI202201', 7, '2022', '电子信息工程学院2022级1班'),
('电子2022级2班', 'EI202202', 7, '2022', '电子信息工程学院2022级2班');

-- 科目数据
INSERT INTO `subject` (`name`, `code`, `description`, `credits`, `department_id`, `sort_order`) VALUES
('Java程序设计', 'JAVA', 'Java编程语言基础与进阶', 3, 1, 1),
('数据结构', 'DS', '数据结构与算法', 3, 1, 2),
('数据库原理', 'DB', '数据库系统原理与应用', 3, 1, 3),
('计算机网络', 'NET', '计算机网络基础与协议', 3, 1, 4),
('操作系统', 'OS', '操作系统原理与实践', 3, 1, 5),
('软件工程', 'SE', '软件工程方法论', 3, 1, 6),
('Web前端开发', 'WEB', 'HTML/CSS/JavaScript前端技术', 3, 1, 7),
('Python编程', 'PY', 'Python语言程序设计', 3, 1, 8)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 教师数据
INSERT INTO `teacher` (`teacher_no`, `real_name`, `gender`, `phone`, `email`, `department_id`, `user_id`) 
VALUES ('T001', '张老师', 1, '13800138001', 'teacher1@exam.com', 1, 2);

-- 学生数据
INSERT INTO `student` (`student_no`, `real_name`, `gender`, `phone`, `email`, `class_id`, `department_id`, `user_id`) 
VALUES ('S2021001', '李四', 1, '13800138002', 'student1@exam.com', 1, 1, 3);

-- 教师班级关联数据（张老师负责计算机2021级1班和2班）
INSERT INTO `teacher_class` (`teacher_id`, `class_id`, `subject`) VALUES 
(1, 1, 'Java程序设计'),
(1, 2, 'Java程序设计');
