import request from '@/utils/request'

// 获取教师仪表盘统计数据
export function getTeacherDashboardStats() {
  return request({
    url: '/teacher/dashboard/stats',
    method: 'get'
  })
}

// 获取待办事项列表
export function getTodos() {
  return request({
    url: '/teacher/dashboard/todos',
    method: 'get'
  })
}

// 获取近期考试列表
export function getRecentExams() {
  return request({
    url: '/teacher/dashboard/recent-exams',
    method: 'get'
  })
}

// 获取教学数据概览
export function getTeachingData() {
  return request({
    url: '/teacher/dashboard/teaching-data',
    method: 'get'
  })
}
