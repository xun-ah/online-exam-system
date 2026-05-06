import request from '@/utils/request'

// 获取学生列表
export function getStudentList(params) {
  return request({
    url: '/admin/students',
    method: 'get',
    params
  })
}

// 创建学生
export function createStudent(data) {
  return request({
    url: '/admin/students',
    method: 'post',
    data
  })
}

// 更新学生
export function updateStudent(id, data) {
  return request({
    url: `/admin/students/${id}`,
    method: 'put',
    data
  })
}

// 删除学生
export function deleteStudent(id) {
  return request({
    url: `/admin/students/${id}`,
    method: 'delete'
  })
}

// 获取教师列表
export function getTeacherList(params) {
  return request({
    url: '/admin/teachers',
    method: 'get',
    params
  })
}

// 创建教师
export function createTeacher(data) {
  return request({
    url: '/admin/teachers',
    method: 'post',
    data
  })
}

// 更新教师
export function updateTeacher(id, data) {
  return request({
    url: `/admin/teachers/${id}`,
    method: 'put',
    data
  })
}

// 删除教师
export function deleteTeacher(id) {
  return request({
    url: `/admin/teachers/${id}`,
    method: 'delete'
  })
}

// 获取院系列表
export function getDepartmentList(params) {
  return request({
    url: '/admin/departments',
    method: 'get',
    params
  })
}

// 创建院系
export function createDepartment(data) {
  return request({
    url: '/admin/departments',
    method: 'post',
    data
  })
}

// 更新院系
export function updateDepartment(id, data) {
  return request({
    url: `/admin/departments/${id}`,
    method: 'put',
    data
  })
}

// 删除院系
export function deleteDepartment(id) {
  return request({
    url: `/admin/departments/${id}`,
    method: 'delete'
  })
}

// 获取班级列表
export function getClassList(params) {
  return request({
    url: '/admin/classes',
    method: 'get',
    params
  })
}

// 创建班级
export function createClass(data) {
  return request({
    url: '/admin/classes',
    method: 'post',
    data
  })
}

// 更新班级
export function updateClass(id, data) {
  return request({
    url: `/admin/classes/${id}`,
    method: 'put',
    data
  })
}

// 删除班级
export function deleteClass(id) {
  return request({
    url: `/admin/classes/${id}`,
    method: 'delete'
  })
}

// 获取系统日志
export function getSystemLogs(params) {
  return request({
    url: '/admin/logs/system',
    method: 'get',
    params
  })
}

// 获取仪表盘统计数据
export function getDashboardStats() {
  return request({
    url: '/admin/statistics/dashboard',
    method: 'get'
  })
}

// 获取各院系考试参与情况
export function getDepartmentExamStats() {
  return request({
    url: '/admin/statistics/department-exam',
    method: 'get'
  })
}

// 获取题型分布统计
export function getQuestionTypeStats() {
  return request({
    url: '/admin/statistics/question-type',
    method: 'get'
  })
}

// 获取月度考试趋势
export function getMonthlyTrend() {
  return request({
    url: '/admin/statistics/monthly-trend',
    method: 'get'
  })
}
