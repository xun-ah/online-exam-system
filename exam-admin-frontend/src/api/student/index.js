import request from '@/utils/request'

// ==================== 待考考试 ====================

// 获取待考考试列表
export function getPendingExamList(params) {
  return request({
    url: '/student/exams/pending',
    method: 'get',
    params
  })
}

// ==================== 在线考试 ====================

// 获取考试详情
export function getExamDetail(examId) {
  return request({
    url: `/student/exams/${examId}`,
    method: 'get'
  })
}

// 开始考试
export function startExam(examId) {
  return request({
    url: `/student/exams/${examId}/start`,
    method: 'post'
  })
}

// 保存答案
export function submitAnswer(data) {
  return request({
    url: '/student/exams/save-answer',
    method: 'post',
    data
  })
}

// 提交试卷
export function submitExam(examId, data) {
  return request({
    url: `/student/exams/${examId}/submit`,
    method: 'post',
    data
  })
}

// ==================== 考试记录 ====================

// 获取考试记录列表
export function getExamRecordList(params) {
  return request({
    url: '/student/exams/records',
    method: 'get',
    params
  })
}

// 获取考试详情（含答卷）
export function getExamRecordDetail(recordId) {
  return request({
    url: `/student/exams/records/${recordId}`,
    method: 'get'
  })
}

// ==================== 错题本 ====================

// 获取错题列表
export function getWrongBookList(params) {
  return request({
    url: '/student/wrong-book',
    method: 'get',
    params
  })
}

// 获取错题详情
export function getWrongQuestionDetail(wrongId) {
  return request({
    url: `/student/wrong-book/${wrongId}`,
    method: 'get'
  })
}

// ==================== 成绩单 ====================

// 获取个人成绩列表
export function getScoreList(params) {
  return request({
    url: '/student/scores',
    method: 'get',
    params
  })
}

// 获取成绩趋势分析
export function getScoreTrend() {
  return request({
    url: '/student/score/trend',
    method: 'get'
  })
}

// 获取知识点掌握雷达图
export function getKnowledgeRadar() {
  return request({
    url: '/student/scores/knowledge-radar',
    method: 'get'
  })
}

// ==================== 个人信息 ====================

// 获取个人信息
export function getProfile() {
  return request({
    url: '/student/profile',
    method: 'get'
  })
}

// 修改密码
export function changePassword(data) {
  return request({
    url: '/student/profile/change-password',
    method: 'post',
    data
  })
}
