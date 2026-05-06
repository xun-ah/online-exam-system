import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  // 从localStorage恢复登录状态
  const storedToken = localStorage.getItem('token') || ''
  const storedUserInfo = localStorage.getItem('userInfo')
  
  console.log('[UserStore] 初始化 - token:', storedToken ? '存在' : '不存在')
  console.log('[UserStore] 初始化 - userInfo:', storedUserInfo)
  
  const token = ref(storedToken)
  const userInfo = ref(storedUserInfo ? JSON.parse(storedUserInfo) : null)

  function setToken(newToken) {
    console.log('[UserStore] 设置token')
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setUserInfo(info) {
    console.log('[UserStore] 设置userInfo:', info)
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  function logout() {
    console.log('[UserStore] 退出登录')
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return { token, userInfo, setToken, setUserInfo, logout }
})
