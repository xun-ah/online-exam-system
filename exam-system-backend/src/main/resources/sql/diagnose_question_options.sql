-- 诊断题目选项数据
-- 查看多选题的选项数据是否为空

-- 1. 查看所有多选题及其选项数据
SELECT 
  id,
  question_no,
  type,
  content,
  options,
  CASE 
    WHEN options IS NULL OR options = '' THEN '选项为空'
    WHEN options NOT LIKE '%A%' THEN '缺少A选项'
    WHEN options NOT LIKE '%B%' THEN '缺少B选项'
    ELSE '选项正常'
  END AS option_status
FROM question
WHERE type IN (1, 2)  -- 单选题和多选题
  AND deleted = 0
ORDER BY id;

-- 2. 统计选项为空的题目数量
SELECT 
  type,
  COUNT(*) AS total_questions,
  SUM(CASE WHEN options IS NULL OR options = '' THEN 1 ELSE 0 END) AS empty_options,
  SUM(CASE WHEN options IS NOT NULL AND options != '' THEN 1 ELSE 0 END) AS has_options
FROM question
WHERE type IN (1, 2)
  AND deleted = 0
GROUP BY type;

-- 3. 查看具体的选项JSON格式示例
SELECT 
  id,
  question_no,
  LEFT(content, 50) AS content_preview,
  options
FROM question
WHERE type IN (1, 2)
  AND deleted = 0
  AND options IS NOT NULL
  AND options != ''
LIMIT 5;
