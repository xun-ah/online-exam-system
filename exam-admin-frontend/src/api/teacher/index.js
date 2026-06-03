import request from '@/utils/request'

// ==================== 题库管理 ====================

// 获取题目列表
export function getQuestionList(params) {
  return request({
    url: '/teacher/questions',
    method: 'get',
    params
  })
}

// 创建题目
export function createQuestion(data) {
  return request({
    url: '/teacher/questions',
    method: 'post',
    data
  })
}

// 更新题目
export function updateQuestion(id, data) {
  return request({
    url: `/teacher/questions/${id}`,
    method: 'put',
    data
  })
}

// 删除题目
export function deleteQuestion(id) {
  return request({
    url: `/teacher/questions/${id}`,
    method: 'delete'
  })
}

// 批量删除题目
export function batchDeleteQuestions(ids) {
  return request({
    url: '/teacher/questions/batch-delete',
    method: 'post',
    data: ids
  })
}

// 批量导入题目
export function batchImportQuestions(file, subject) {
  const formData = new FormData()
  formData.append('file', file)
  if (subject) {
    formData.append('subject', subject)
  }
  return request({
    url: '/teacher/questions/batch-import',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 批量导出题目
export function exportQuestions(params) {
  return request({
    url: '/teacher/questions/batch-export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

// ==================== 试卷管理 ====================

// 获取试卷列表
export function getPaperList(params) {
  return request({
    url: '/teacher/papers',
    method: 'get',
    params
  })
}

// 创建试卷
export function createPaper(data) {
  return request({
    url: '/teacher/papers',
    method: 'post',
    data
  })
}

// 更新试卷
export function updatePaper(id, data) {
  return request({
    url: `/teacher/papers/${id}`,
    method: 'put',
    data
  })
}

// 删除试卷
export function deletePaper(id) {
  return request({
    url: `/teacher/papers/${id}`,
    method: 'delete'
  })
}

// 手动组卷
export function manualComposePaper(data) {
  return request({
    url: '/teacher/papers/manual-compose',
    method: 'post',
    data
  })
}

// 自动组卷
export function autoComposePaper(data) {
  return request({
    url: '/teacher/papers/auto-compose',
    method: 'post',
    data
  })
}

// 获取试卷题目列表（用于预览）
export function getPaperQuestions(paperId) {
  return request({
    url: `/teacher/papers/${paperId}/questions`,
    method: 'get'
  })
}

// ==================== 考试管理 ====================

// 获取考试列表
export function getExamList(params) {
  return request({
    url: '/teacher/exams',
    method: 'get',
    params
  })
}

// 获取考试详情
export function getExamDetail(id) {
  return request({
    url: `/teacher/exams/${id}`,
    method: 'get'
  })
}

// 获取待阅卷列表
export function getPendingGrading(params) {
  return request({
    url: '/teacher/grading/pending',
    method: 'get',
    params
  })
}

// 获取已阅卷列表
export function getGradedRecords(params) {
  return request({
    url: '/teacher/grading/graded',
    method: 'get',
    params
  })
}

// 获取待阅试卷详情
export function getGradingDetail(recordId) {
  return request({
    url: `/teacher/grading/${recordId}`,
    method: 'get'
  })
}

// 提交阅卷评分
export function submitGrading(data) {
  return request({
    url: '/teacher/grading/submit',
    method: 'post',
    data
  })
}

// 获取成绩统计
export function getScoreStatistics(examId) {
  return request({
    url: `/teacher/grading/statistics/${examId}`,
    method: 'get'
  })
}

// 获取错题分析
export function getErrorAnalysis(examId) {
  return request({
    url: `/teacher/grading/error-analysis/${examId}`,
    method: 'get'
  })
}

// 导出成绩
export function exportExamScores(params) {
  return request({
    url: '/teacher/grading/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

// 更新考试
export function updateExam(id, data) {
  return request({
    url: `/teacher/exams/${id}`,
    method: 'put',
    data
  })
}

// 发布考试
export function publishExam(data) {
  return request({
    url: '/teacher/exams',
    method: 'post',
    data
  })
}

// 删除考试
export function deleteExam(id) {
  return request({
    url: `/teacher/exams/${id}`,
    method: 'delete'
  })
}

// 获取考试监控数据
export function getExamMonitor(examId) {
  return request({
    url: `/teacher/exams/${examId}/monitor`,
    method: 'get'
  })
}

// 手动延长考试时间
export function extendExamTime(data) {
  return request({
    url: '/teacher/exams/extend-time',
    method: 'post',
    data
  })
}

// 强制学生交卷
export function forceSubmitExam(examId, studentId) {
  return request({
    url: `/teacher/exams/${examId}/force-submit/${studentId}`,
    method: 'post'
  })
}

// 打回考试记录（让学生重做）
export function rollbackExamRecord(recordId, reason) {
  return request({
    url: `/teacher/grading/${recordId}/rollback`,
    method: 'post',
    data: { reason }
  })
}

// 获取我的班级列表
export function getMyClasses() {
  return request({
    url: '/teacher/exams/my-classes',
    method: 'get'
  })
}

// ==================== 阅卷与成绩管理 ====================

// 获取成绩列表
export function getScoreList(params) {
  return request({
    url: '/teacher/scores',
    method: 'get',
    params
  })
}

// 获取错题分析
export function getWrongAnalysis(examId) {
  return request({
    url: `/teacher/exams/${examId}/wrong-analysis`,
    method: 'get'
  })
}

// 导出成绩
export function exportScores(examId) {
  return request({
    url: `/teacher/exams/${examId}/export-scores`,
    method: 'get',
    responseType: 'blob'
  })
}

// ==================== 学生管理 ====================

// 获取学生列表
export function getStudentList(params) {
  return request({
    url: '/teacher/students',
    method: 'get',
    params
  })
}

// 根据班级ID获取学生列表
export function getStudentsByClassId(classId) {
  return request({
    url: '/teacher/students',
    method: 'get',
    params: { classId, pageNum: 1, pageSize: 100 }
  })
}

// ==================== 科目管理 ====================

// 获取科目列表（教师任教的科目）
export function getSubjectList() {
  return request({
    url: '/subject/list',
    method: 'get'
  })
}

// 获取所有科目（包括禁用的，管理员使用）
export function getAllSubjects() {
  return request({
    url: '/subject/all',
    method: 'get'
  })
}

// ==================== 成绩分析 ====================

// 班级间成绩对比
export function getClassComparison(examId) {
  return request({
    url: '/teacher/score-analysis/class-comparison',
    method: 'get',
    params: { examId }
  })
}

// 知识点掌握度分析
export function getKnowledgeAnalysis(examId) {
  return request({
    url: '/teacher/score-analysis/knowledge-analysis',
    method: 'get',
    params: { examId }
  })
}

// 试题质量分析
export function getQuestionQuality(examId) {
  return request({
    url: '/teacher/score-analysis/question-quality',
    method: 'get',
    params: { examId }
  })
}