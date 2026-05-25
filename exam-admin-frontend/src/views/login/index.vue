<template>
  <div class="login-container">
    <!-- 左侧展示区 -->
    <div class="left-section">
      <div class="left-content">
        <div class="system-title">
          <h1>在线考试系统</h1>
          <p>Online Examination System</p>
        </div>
        
        <div class="features">
          <div class="feature-item" v-for="(item, index) in features" :key="index">
            <div class="feature-icon">
              <el-icon><Check /></el-icon>
            </div>
            <div class="feature-text">
              <h3>{{ item.title }}</h3>
              <p>{{ item.desc }}</p>
            </div>
          </div>
        </div>
        
        <div class="footer">
          <p>© 2026 在线考试系统 v2.0 | 技术支持：教务处</p>
        </div>
      </div>
    </div>
    
    <!-- 右侧登录区 -->
    <div class="right-section">
      <div class="login-box">
        <div class="login-header">
          <div class="header-bar"></div>
          <h2>欢迎登录</h2>
          <p>请选择您的身份并输入账号密码</p>
        </div>
        
        <el-form :model="loginForm" :rules="rules" ref="formRef" class="login-form">
          <!-- 身份选择 -->
          <div class="role-selector">
            <label class="form-label">登录身份</label>
            <div class="role-buttons">
              <div 
                class="role-btn" 
                :class="{ active: loginForm.role === 'admin' }"
                @click="loginForm.role = 'admin'"
              >
                管理员
              </div>
              <div 
                class="role-btn" 
                :class="{ active: loginForm.role === 'teacher' }"
                @click="loginForm.role = 'teacher'"
              >
                教师
              </div>
              <div 
                class="role-btn" 
                :class="{ active: loginForm.role === 'student' }"
                @click="loginForm.role = 'student'"
              >
                学生
              </div>
            </div>
          </div>
          
          <!-- 账号 -->
          <div class="form-item">
            <label class="form-label">账号</label>
            <el-input
              v-model="loginForm.username"
              placeholder="请输入账号/工号/学号"
              size="large"
              class="custom-input"
              clearable
            />
          </div>
          
          <!-- 密码 -->
          <div class="form-item">
            <label class="form-label">密码</label>
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              show-password
              class="custom-input"
              @keyup.enter="handleLogin"
              clearable
            />
          </div>
          
          <!-- 验证码 -->
          <div class="form-item">
            <label class="form-label">验证码</label>
            <div class="captcha-row">
              <el-input
                v-model="loginForm.captcha"
                placeholder="请输入验证码"
                size="large"
                class="custom-input captcha-input"
                @keyup.enter="handleLogin"
                clearable
              />
              <div class="captcha-code" @click="refreshCaptcha" :title="'点击刷新验证码'">
                {{ captchaCode }}
              </div>
            </div>
          </div>
          
          <!-- 记住密码和忘记密码 -->
          <div class="form-options">
            <el-checkbox v-model="loginForm.remember">记住密码</el-checkbox>
            <el-link type="primary" :underline="false" class="forgot-link" @click="handleForgotPassword">忘记密码？</el-link>
          </div>
          
          <!-- 登录按钮 -->
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleLogin"
            class="login-button"
          >
            登 录
          </el-button>
          
          <!-- 其他登录方式 -->
          <div class="other-login">
            <div class="divider">
              <span>其他登录方式</span>
            </div>
            <div class="login-methods">
              <div class="method-item">
                <div class="method-icon wechat">企</div>
                <span>企业微信</span>
              </div>
              <div class="method-item">
                <div class="method-icon dingtalk">钉</div>
                <span>钉钉</span>
              </div>
              <div class="method-item">
                <div class="method-icon student">学</div>
                <span>学号快捷</span>
              </div>
            </div>
          </div>
          
          <!-- 底部链接 -->
          <div class="bottom-links">
            <span>还没有账号？</span>
            <el-link type="primary" :underline="false" class="contact-link" @click="handleContactAdmin">联系管理员</el-link>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import { login, getCaptcha } from '@/api/auth'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const captchaCode = ref('----')

const features = [
  { title: '智能组卷', desc: '支持手动/自动组卷，多种题型灵活配置' },
  { title: '实时监控', desc: '考试过程全程监控，异常行为自动预警' },
  { title: '多维分析', desc: '成绩统计、知识点掌握度、错题分析' },
  { title: '防作弊', desc: '切屏检测、题目乱序、答题时间分析' }
]

const loginForm = reactive({
  username: '',
  password: '',
  captcha: '',
  role: 'admin',
  remember: false
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

// 刷新验证码
const refreshCaptcha = async () => {
  try {
    // 传递username参数，让后端将验证码与该用户名绑定
    const res = await getCaptcha({ username: loginForm.username })
    captchaCode.value = res.data.captcha
    console.log('验证码获取成功:', res.data.captcha)
  } catch (error) {
    console.error('获取验证码失败:', error)
    ElMessage.error('获取验证码失败，请检查后端服务是否启动')
    // 失败时使用前端随机生成
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
    let code = ''
    for (let i = 0; i < 4; i++) {
      code += chars.charAt(Math.floor(Math.random() * chars.length))
    }
    captchaCode.value = code
  }
}

// 登录处理
const handleLogin = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        console.log('开始登录，角色:', loginForm.role)
        console.log('登录信息:', {
          username: loginForm.username,
          captcha: loginForm.captcha
        })
        
        // 角色映射：前端字符串 -> 后端数字
        const roleMap = {
          'admin': 1,
          'teacher': 2,
          'student': 3
        }
        
        // 调用后端登录接口
        const res = await login({
          username: loginForm.username,
          password: loginForm.password,
          captcha: loginForm.captcha,
          role: roleMap[loginForm.role] // 发送对应的角色ID
        })
        
        console.log('登录成功，返回数据:', res)
        console.log('登录成功，res.data:', res.data)
        
        // 检查res.data是否存在
        if (!res.data || !res.data.token) {
          console.error('[Login] 登录返回数据异常:', res)
          ElMessage.error('登录返回数据异常')
          return
        }
        
        // 处理记住密码（仅记住账号和角色）
        if (loginForm.remember) {
          localStorage.setItem('rememberedUsername', loginForm.username)
          localStorage.setItem('rememberedRole', loginForm.role)
          // 清除可能存在的旧密码记录
          localStorage.removeItem('rememberedPassword')
        } else {
          localStorage.removeItem('rememberedUsername')
          localStorage.removeItem('rememberedRole')
          localStorage.removeItem('rememberedPassword')
        }
        
        // 保存Token和用户信息
        console.log('[Login] 准备保存token:', res.data.token)
        console.log('[Login] 准备保存userInfo:', {
          id: res.data.userId,
          username: res.data.username,
          realName: res.data.realName,
          role: res.data.role
        })
        
        userStore.setToken(res.data.token)
        userStore.setUserInfo({
          id: res.data.userId,
          username: res.data.username,
          realName: res.data.realName,
          role: res.data.role,
          avatar: res.data.avatar || '' // 保存头像信息
        })
        
        // 验证是否保存成功
        console.log('[Login] 保存后 - localStorage token:', localStorage.getItem('token'))
        console.log('[Login] 保存后 - localStorage userInfo:', localStorage.getItem('userInfo'))
        console.log('[Login] 保存后 - userStore token:', userStore.token)
        console.log('[Login] 保存后 - userStore userInfo:', userStore.userInfo)
        
        ElMessage.success('登录成功')
                
        // 根据角色跳转到不同页面
        const roleRoutes = {
          1: '/admin/dashboard',   // 管理员
          2: '/teacher/dashboard', // 教师
          3: '/student/dashboard'  // 学生
        }
                
        const targetRoute = roleRoutes[res.data.role]
        if (targetRoute) {
          console.log('跳转到:', targetRoute)
          // 使用 router.push 进行跳转,避免页面刷新
          setTimeout(() => {
            router.push(targetRoute)
          }, 300)
        } else {
          ElMessage.error('未知角色,请联系管理员')
        }
      } catch (error) {
        console.error('登录失败:', error)
        ElMessage.error(error.response?.data?.message || '登录失败，请检查账号密码')
        // 登录失败后刷新验证码
        refreshCaptcha()
        // 清空验证码输入框
        loginForm.captcha = ''
      } finally {
        loading.value = false
      }
    }
  })
}

// 忘记密码
const handleForgotPassword = () => {
  router.push('/forgot-password')
}

// 联系管理员
const handleContactAdmin = () => {
  ElMessage.info('请联系系统管理员：admin@exam.com 或拨打 13800138000')
}

// 监听用户名变化，自动刷新验证码
import { watch } from 'vue'

watch(() => loginForm.username, () => {
  if (loginForm.username) {
    refreshCaptcha()
  }
})

onMounted(() => {
  console.log('登录页面加载完成')
  refreshCaptcha()
  
  // 加载记住的账号
  const rememberedUsername = localStorage.getItem('rememberedUsername')
  const rememberedRole = localStorage.getItem('rememberedRole')
  
  if (rememberedUsername) {
    loginForm.username = rememberedUsername
    loginForm.remember = true
    if (rememberedRole) {
      loginForm.role = rememberedRole
    }
    // 如果有记住的用户名，延迟刷新验证码（在服务检测后）
    setTimeout(() => {
      if (serverOnline.value) {
        refreshCaptcha()
      }
    }, 500)
  }
})
</script>

<style scoped lang="scss">
.login-container {
  display: flex;
  min-height: 100vh;
  background: #f0f2f5;
}

// 左侧展示区
.left-section {
  flex: 1;
  background: linear-gradient(135deg, #001529 0%, #002140 100%);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  
  &::before,
  &::after {
    content: '';
    position: absolute;
    border-radius: 50%;
    opacity: 0.1;
  }
  
  &::before {
    width: 450px;
    height: 280px;
    background: #0a3d6b;
    top: 15%;
    left: 5%;
    transform: rotate(-15deg);
  }
  
  &::after {
    width: 250px;
    height: 180px;
    background: #1a4d3e;
    bottom: 25%;
    right: 15%;
    transform: rotate(20deg);
  }
  
  .left-content {
    position: relative;
    z-index: 1;
    color: #fff;
    padding: 60px;
    max-width: 600px;
    
    .system-title {
      margin-bottom: 80px;
      background: rgba(255, 255, 255, 0.08);
      backdrop-filter: blur(10px);
      padding: 32px 24px;
      border-radius: 16px;
      border: 1px solid rgba(255, 255, 255, 0.1);
      
      h1 {
        font-size: 36px;
        font-weight: 600;
        margin-bottom: 12px;
        text-align: center;
        letter-spacing: 2px;
      }
      
      p {
        font-size: 16px;
        opacity: 0.7;
        text-align: center;
        letter-spacing: 1px;
      }
    }
    
    .features {
      .feature-item {
        display: flex;
        align-items: center;
        margin-bottom: 36px;
        
        .feature-icon {
          width: 72px;
          height: 36px;
          background: rgba(24, 144, 255, 0.15);
          border-radius: 18px;
          display: flex;
          align-items: center;
          justify-content: center;
          margin-right: 20px;
          flex-shrink: 0;
          
          .el-icon {
            font-size: 16px;
            color: #1890ff;
          }
        }
        
        .feature-text {
          h3 {
            font-size: 16px;
            margin-bottom: 4px;
            font-weight: 500;
            color: #fff;
          }
          
          p {
            font-size: 13px;
            opacity: 0.6;
            line-height: 1.5;
            color: rgba(255, 255, 255, 0.6);
          }
        }
      }
    }
    
    .footer {
      margin-top: 80px;
      text-align: center;
      font-size: 14px;
      opacity: 0.6;
    }
  }
}

// 右侧登录区
.right-section {
  width: 600px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  
  .login-box {
    width: 100%;
    max-width: 480px;
    background: #fff;
    border-radius: 16px;
    padding: 40px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    
    .login-header {
      margin-bottom: 32px;
      text-align: center;
      
      .header-bar {
        height: 4px;
        background: #1890ff;
        border-radius: 2px;
        margin-bottom: 24px;
      }
      
      h2 {
        font-size: 28px;
        color: #262626;
        margin-bottom: 12px;
        font-weight: 600;
      }
      
      p {
        font-size: 14px;
        color: #8c8c8c;
      }
    }
    
    .login-form {
      .role-selector {
        margin-bottom: 24px;
        
        .form-label {
          display: block;
          font-size: 14px;
          color: #262626;
          margin-bottom: 12px;
          font-weight: 500;
        }
        
        .role-buttons {
          display: flex;
          gap: 12px;
          
          .role-btn {
            flex: 1;
            padding: 12px;
            text-align: center;
            background: #f5f5f5;
            border: 1px solid #d9d9d9;
            border-radius: 6px;
            cursor: pointer;
            transition: all 0.3s;
            font-size: 14px;
            color: #595959;
            
            &:hover {
              border-color: #1890ff;
              color: #1890ff;
            }
            
            &.active {
              border-color: #1890ff;
              color: #fff;
            }
            
            // 管理员选中时为红色
            &:first-child.active {
              background: #ff4d4f;
              border-color: #ff4d4f;
            }
            
            // 教师选中时为蓝色
            &:nth-child(2).active {
              background: #1890ff;
              border-color: #1890ff;
            }
            
            // 学生选中时为绿色
            &:nth-child(3).active {
              background: #52c41a;
              border-color: #52c41a;
            }
          }
        }
      }
      
      .form-item {
        margin-bottom: 20px;
        
        .form-label {
          display: block;
          font-size: 14px;
          color: #262626;
          margin-bottom: 8px;
          font-weight: 500;
        }
        
        .custom-input {
          :deep(.el-input__wrapper) {
            background: #f5f5f5;
            border: 1px solid #e8e8e8;
            border-radius: 6px;
            box-shadow: none;
            
            &:hover {
              border-color: #1890ff;
            }
            
            &.is-focus {
              border-color: #1890ff;
              background: #fff;
            }
          }
        }
        
        .captcha-row {
          display: flex;
          gap: 12px;
          
          .captcha-input {
            flex: 1;
          }
          
          .captcha-code {
            width: 120px;
            height: 40px;
            background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%);
            border: 1px solid #1890ff;
            border-radius: 6px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 18px;
            font-weight: 600;
            color: #1890ff;
            cursor: pointer;
            user-select: none;
            letter-spacing: 2px;
            transition: all 0.3s;
            
            &:hover {
              background: linear-gradient(135deg, #bae7ff 0%, #91d5ff 100%);
            }
          }
        }
      }
      
      .form-options {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 20px;
        font-size: 14px;
        
        .forgot-link {
          font-size: 14px;
        }
      }
      
      .login-button {
        width: 100%;
        height: 48px;
        font-size: 16px;
        border-radius: 8px;
        background: #1890ff;
        border: none;
        
        &:hover {
          background: #40a9ff;
        }
      }
      
      // 其他登录方式
      .other-login {
        margin-top: 32px;
        
        .divider {
          display: flex;
          align-items: center;
          margin: 24px 0;
          
          &::before,
          &::after {
            content: '';
            flex: 1;
            height: 1px;
            background: #e8e8e8;
          }
          
          span {
            padding: 0 16px;
            font-size: 13px;
            color: #8c8c8c;
          }
        }
        
        .login-methods {
          display: flex;
          justify-content: center;
          gap: 32px;
          
          .method-item {
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 8px;
            cursor: pointer;
            
            .method-icon {
              width: 40px;
              height: 40px;
              border-radius: 50%;
              display: flex;
              align-items: center;
              justify-content: center;
              font-size: 16px;
              font-weight: 500;
              
              &.wechat {
                background: #e6f7e6;
                color: #52c41a;
              }
              
              &.dingtalk {
                background: #e6f0ff;
                color: #1890ff;
              }
              
              &.student {
                background: #fff2e6;
                color: #fa8c16;
              }
            }
            
            span {
              font-size: 12px;
              color: #595959;
            }
            
            &:hover .method-icon {
              transform: scale(1.05);
              transition: all 0.3s;
            }
          }
        }
      }
      
      // 底部链接
      .bottom-links {
        margin-top: 20px;
        text-align: center;
        font-size: 14px;
        color: #595959;
        
        .contact-link {
          margin-left: 8px;
          font-size: 14px;
        }
      }
    }
  }
}

@media (max-width: 1200px) {
  .left-section {
    display: none;
  }
  
  .right-section {
    width: 100%;
  }
}
</style>
