import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  // 从sessionStorage恢复登录状态（每个标签页独立）
  const storedToken = sessionStorage.getItem('token') || ''
  const storedUserInfo = sessionStorage.getItem('userInfo')
  
  console.log('[UserStore] 初始化 - token:', storedToken ? '存在' : '不存在')
  console.log('[UserStore] 初始化 - userInfo:', storedUserInfo)
  
  const token = ref(storedToken)
  const userInfo = ref(storedUserInfo ? JSON.parse(storedUserInfo) : null)

  function setToken(newToken) {
    console.log('[UserStore] 设置token')
    token.value = newToken
    sessionStorage.setItem('token', newToken)
  }

  function setUserInfo(info) {
    console.log('[UserStore] 设置userInfo:', info)
    userInfo.value = info
    sessionStorage.setItem('userInfo', JSON.stringify(info))
  }

  function logout() {
    console.log('[UserStore] 退出登录')
    token.value = ''
    userInfo.value = null
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('userInfo')
  }

  return { token, userInfo, setToken, setUserInfo, logout }
})
