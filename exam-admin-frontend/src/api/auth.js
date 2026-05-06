import request from '@/utils/request'

// 获取验证码
export function getCaptcha() {
  return request({
    url: '/auth/captcha',
    method: 'get'
  }).catch(error => {
    console.error('获取验证码接口调用失败:', error)
    throw error
  })
}

// 登录
export function login(data) {
  console.log('调用登录接口，参数:', data)
  return request({
    url: '/auth/login',
    method: 'post',
    data
  }).catch(error => {
    console.error('登录接口调用失败:', error)
    throw error
  })
}

// 获取用户信息
export function getUserInfo() {
  return request({
    url: '/auth/info',
    method: 'get'
  })
}

// 登出
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}
