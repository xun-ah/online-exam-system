<template>
  <div class="forgot-password-container">
    <div class="left-section">
      <div class="left-content">
        <div class="system-title">
          <h1>在线考试系统</h1>
          <p>Online Examination System</p>
        </div>
        
        <div class="footer">
          <p>© 2026 在线考试系统 v2.0 | 技术支持：教务处</p>
        </div>
      </div>
    </div>
    
    <div class="right-section">
      <div class="reset-box">
        <div class="reset-header">
          <div class="header-bar"></div>
          <h2>重置密码</h2>
          <p>请输入您的账号信息重置密码</p>
        </div>
        
        <el-form :model="resetForm" :rules="rules" ref="formRef" class="reset-form">
          <!-- 身份选择 -->
          <div class="role-selector">
            <label class="form-label">身份</label>
            <div class="role-buttons">
              <div 
                class="role-btn" 
                :class="{ active: resetForm.role === 'student' }"
                @click="resetForm.role = 'student'"
              >
                学生
              </div>
              <div 
                class="role-btn" 
                :class="{ active: resetForm.role === 'teacher' }"
                @click="resetForm.role = 'teacher'"
              >
                教师
              </div>
            </div>
          </div>
          
          <!-- 账号 -->
          <div class="form-item">
            <label class="form-label">学号/工号</label>
            <el-input
              v-model="resetForm.username"
              placeholder="请输入学号或工号"
              size="large"
              class="custom-input"
              clearable
              @blur="refreshCaptcha"
            />
          </div>
          
          <!-- 手机号 -->
          <div class="form-item">
            <label class="form-label">注册手机号</label>
            <el-input
              v-model="resetForm.phone"
              placeholder="请输入注册时使用的手机号"
              size="large"
              class="custom-input"
              clearable
            />
          </div>
          
          <!-- 验证码 -->
          <div class="form-item">
            <label class="form-label">验证码</label>
            <div class="captcha-row">
              <el-input
                v-model="resetForm.captcha"
                placeholder="请输入验证码"
                size="large"
                class="custom-input captcha-input"
                clearable
              />
              <div class="captcha-code" @click="refreshCaptcha">
                {{ captchaCode }}
              </div>
            </div>
          </div>
          
          <!-- 新密码 -->
          <div class="form-item">
            <label class="form-label">新密码</label>
            <el-input
              v-model="resetForm.newPassword"
              type="password"
              placeholder="请输入新密码（6-20位字母数字组合）"
              size="large"
              show-password
              class="custom-input"
              clearable
            />
          </div>
          
          <!-- 确认新密码 -->
          <div class="form-item">
            <label class="form-label">确认新密码</label>
            <el-input
              v-model="resetForm.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              size="large"
              show-password
              class="custom-input"
              clearable
            />
          </div>
          
          <!-- 重置按钮 -->
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleReset"
            class="reset-button"
          >
            确认重置
          </el-button>
          
          <!-- 返回登录 -->
          <div class="back-login">
            <el-link type="primary" :underline="false" @click="goToLogin">
              <el-icon><ArrowLeft /></el-icon>
              返回登录
            </el-link>
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
import { ArrowLeft } from '@element-plus/icons-vue'
import { getCaptcha } from '@/api/auth'
import request from '@/utils/request'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const captchaCode = ref('----')

const resetForm = reactive({
  username: '',
  phone: '',
  captcha: '',
  newPassword: '',
  confirmPassword: '',
  role: 'student'
})

const rules = {
  username: [{ required: true, message: '请输入学号或工号', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== resetForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 刷新验证码
const refreshCaptcha = async () => {
  try {
    // 如果有学号/工号，传递username参数用于存储验证码
    const params = resetForm.username ? { username: resetForm.username } : {}
    const res = await getCaptcha(params)
    captchaCode.value = res.data.captcha
  } catch (error) {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
    let code = ''
    for (let i = 0; i < 4; i++) {
      code += chars.charAt(Math.floor(Math.random() * chars.length))
    }
    captchaCode.value = code
  }
}

// 重置密码
const handleReset = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const roleMap = {
          'student': 3,
          'teacher': 2
        }
        
        await request.post('/auth/reset-password', {
          username: resetForm.username,
          phone: resetForm.phone,
          captcha: resetForm.captcha,
          role: roleMap[resetForm.role],
          newPassword: resetForm.newPassword
        })
        
        ElMessage.success('密码重置成功，请使用新密码登录')
        setTimeout(() => {
          router.push('/login')
        }, 1500)
      } catch (error) {
        console.error('重置密码失败:', error)
        ElMessage.error(error.response?.data?.message || '重置密码失败，请检查信息')
        refreshCaptcha()
        resetForm.captcha = ''
      } finally {
        loading.value = false
      }
    }
  })
}

// 返回登录
const goToLogin = () => {
  router.push('/login')
}

onMounted(() => {
  refreshCaptcha()
})
</script>

<style scoped lang="scss">
.forgot-password-container {
  display: flex;
  min-height: 100vh;
  background: #f0f2f5;
}

.left-section {
  flex: 1;
  background: linear-gradient(135deg, #001529 0%, #002140 100%);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  
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
    
    .footer {
      margin-top: 80px;
      text-align: center;
      font-size: 14px;
      opacity: 0.6;
    }
  }
}

.right-section {
  width: 600px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  
  .reset-box {
    width: 100%;
    max-width: 480px;
    background: #fff;
    border-radius: 16px;
    padding: 40px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    
    .reset-header {
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
    
    .reset-form {
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
              background: #52c41a;
              border-color: #52c41a;
              color: #fff;
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
      
      .reset-button {
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
      
      .back-login {
        margin-top: 20px;
        text-align: center;
        font-size: 14px;
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
