-- 为 question 表的编程题相关字段添加中文注释
USE exam_system;

-- 添加字段注释
ALTER TABLE `question` 
  MODIFY COLUMN `language` VARCHAR(20) COMMENT '编程语言（编程题专用）：Java、Python、C++ 等',
  MODIFY COLUMN `code_template` TEXT COMMENT '代码模板（编程题专用）：学生初始代码框架',
  MODIFY COLUMN `test_cases` TEXT COMMENT '测试用例（编程题专用）：JSON格式，用于自动判题',
  MODIFY COLUMN `time_limit` INT DEFAULT 1000 COMMENT '时间限制（编程题专用）：单位毫秒，默认1000ms',
  MODIFY COLUMN `memory_limit` INT DEFAULT 256 COMMENT '内存限制（编程题专用）：单位MB，默认256MB';
