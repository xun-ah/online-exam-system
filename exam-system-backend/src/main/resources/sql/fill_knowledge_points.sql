-- 为现有题目批量设置知识点（基于题目内容自动分类）
-- 先清空现有知识点
UPDATE question SET knowledge_point = NULL;

-- 单选题知识点设置
UPDATE question SET knowledge_point = '基础概念' WHERE type = 1 AND (content LIKE '%定义%' OR content LIKE '%概念%' OR content LIKE '%什么%' OR content LIKE '%哪个%');
UPDATE question SET knowledge_point = '语法基础' WHERE type = 1 AND (content LIKE '%语法%' OR content LIKE '%规则%' OR content LIKE '%正确%');
UPDATE question SET knowledge_point = '函数应用' WHERE type = 1 AND (content LIKE '%函数%' OR content LIKE '%方法%' OR content LIKE '%调用%');
UPDATE question SET knowledge_point = '数据结构' WHERE type = 1 AND (content LIKE '%数组%' OR content LIKE '%列表%' OR content LIKE '%栈%' OR content LIKE '%队列%' OR content LIKE '%树%');
UPDATE question SET knowledge_point = '算法基础' WHERE type = 1 AND (content LIKE '%算法%' OR content LIKE '%排序%' OR content LIKE '%查找%' OR content LIKE '%复杂度%');
UPDATE question SET knowledge_point = '面向对象' WHERE type = 1 AND (content LIKE '%类%' OR content LIKE '%对象%' OR content LIKE '%继承%' OR content LIKE '%多态%');

-- 多选题知识点设置
UPDATE question SET knowledge_point = '基础概念' WHERE type = 2 AND (content LIKE '%定义%' OR content LIKE '%概念%' OR content LIKE '%哪些%');
UPDATE question SET knowledge_point = '综合应用' WHERE type = 2 AND (content LIKE '%应用%' OR content LIKE '%实践%' OR content LIKE '%场景%');

-- 判断题知识点设置
UPDATE question SET knowledge_point = '基础概念' WHERE type = 3;

-- 填空题知识点设置
UPDATE question SET knowledge_point = '语法基础' WHERE type = 4 AND (content LIKE '%语法%' OR content LIKE '%关键字%' OR content LIKE '%语句%');
UPDATE question SET knowledge_point = '代码实现' WHERE type = 4 AND (content LIKE '%代码%' OR content LIKE '%程序%' OR content LIKE '%实现%');

-- 简答题知识点设置
UPDATE question SET knowledge_point = '理论理解' WHERE type = 5 AND (content LIKE '%什么%' OR content LIKE '%为什么%' OR content LIKE '%解释%');
UPDATE question SET knowledge_point = '综合分析' WHERE type = 5 AND (content LIKE '%分析%' OR content LIKE '%比较%' OR content LIKE '%区别%');

-- 编程题知识点设置
UPDATE question SET knowledge_point = '编程实践' WHERE type = 6;

-- 为仍未设置知识点的题目按题型设置默认值
UPDATE question SET knowledge_point = '单选题' WHERE type = 1 AND (knowledge_point IS NULL OR knowledge_point = '');
UPDATE question SET knowledge_point = '多选题' WHERE type = 2 AND (knowledge_point IS NULL OR knowledge_point = '');
UPDATE question SET knowledge_point = '判断题' WHERE type = 3 AND (knowledge_point IS NULL OR knowledge_point = '');
UPDATE question SET knowledge_point = '填空题' WHERE type = 4 AND (knowledge_point IS NULL OR knowledge_point = '');
UPDATE question SET knowledge_point = '简答题' WHERE type = 5 AND (knowledge_point IS NULL OR knowledge_point = '');
UPDATE question SET knowledge_point = '编程题' WHERE type = 6 AND (knowledge_point IS NULL OR knowledge_point = '');

-- 验证设置结果
SELECT 
    knowledge_point,
    COUNT(*) as question_count
FROM question
WHERE deleted = 0
GROUP BY knowledge_point
ORDER BY question_count DESC;
