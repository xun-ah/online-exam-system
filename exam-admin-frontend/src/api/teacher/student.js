import request from '@/utils/request'

// 获取学生列表
export function getStudentList(params) {
  return request({
    url: '/teacher/students',
    method: 'get',
    params
  })
}

// 获取学生详情
export function getStudentById(id) {
  return request({
    url: `/teacher/students/${id}`,
    method: 'get'
  })
}

// 获取学生成绩列表
export function getStudentScores(id) {
  return request({
    url: `/teacher/students/${id}/scores`,
    method: 'get'
  })
}

// 导出学生成绩
export function exportScores(params) {
  return request({
    url: '/teacher/students/export',
    method: 'get',
    params
  })
}

// 获取教师所属院系信息
export function getTeacherDepartment() {
  return request({
    url: '/teacher/students/department',
    method: 'get'
  })
}

// 获取本院系下的班级列表
export function getClassListByDepartment(params) {
  return request({
    url: '/teacher/students/classes',
    method: 'get',
    params
  })
}
