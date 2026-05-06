import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  },
  withCredentials: true // 允许跨域请求携带 Cookie/Session
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    console.log('发起请求:', config.method.toUpperCase(), config.url)
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    console.log('收到响应:', response.config.url, response.data)
    const res = response.data
    
    // 后端返回的标准格式
    if (res.code !== 200) {
      // Token失效或后端重启导致认证失败
      if (res.code === 401) {
        ElMessage.error('登录已过期，请重新登录')
        const userStore = useUserStore()
        userStore.logout()
        router.replace('/login')
        return Promise.reject(new Error(res.message || '未授权'))
      }
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    console.error('响应错误:', error)
    
    // 处理不同的错误状态码
    if (error.response) {
      const status = error.response.status
      const message = error.response.data?.message || error.message
      
      switch (status) {
        case 401:
          // 后端服务重启或Token失效，强制重新登录
          ElMessage.error('登录已失效，请重新登录')
          const userStore = useUserStore()
          userStore.logout()
          router.replace('/login')
          break
        case 403:
          ElMessage.error('拒绝访问')
          break
        case 404:
          ElMessage.error('请求资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(message || '请求失败')
      }
    } else if (error.request) {
      // 请求已发送但没有收到响应（后端服务未启动或网络断开）
      console.error('未收到响应:', error.request)
      ElMessage.error('网络连接失败，请检查后端服务是否启动')
      // 注意：这里不清除token，因为可能是临时网络问题
      // 如果后端服务恢复了，刷新页面即可正常使用
    } else {
      // 请求配置时发生错误
      ElMessage.error('请求配置错误')
    }
    
    return Promise.reject(error)
  }
)

export default request
