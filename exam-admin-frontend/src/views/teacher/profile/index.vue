<template>
  <div class="profile-page">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      首页 / 个人中心
    </div>

    <div class="content-wrapper">
      <!-- 上半部分 -->
      <div class="top-section">
        <!-- 左侧：头像 + 用户信息 -->
        <div class="user-info-card">
          <div class="avatar-wrapper">
            <el-upload
              class="avatar-uploader"
              action="#"
              :show-file-list="false"
              :before-upload="handleUploadAvatar"
              accept="image/*"
            >
              <div v-if="profileForm.avatar" class="avatar-image" :style="{ backgroundImage: `url(${profileForm.avatar})` }">
              </div>
              <div v-else class="avatar-circle" :style="{ backgroundColor: avatarColor }">
                {{ userInfo?.realName?.charAt(0) || '教' }}
              </div>
              <div class="avatar-overlay">
                <el-icon><Camera /></el-icon>
                <span>点击更换</span>
              </div>
            </el-upload>
            <h3 class="user-name">{{ userInfo?.realName || '张老师' }}</h3>
            <div class="user-email">{{ userInfo?.email || 'teacher@exam.com' }}</div>
          </div>
          <div class="info-list">
            <div class="info-item">
              <span class="label">工号</span>
              <span class="value">{{ userInfo?.username || 'T2024001' }}</span>
            </div>
            <div class="info-item">
              <span class="label">姓名</span>
              <span class="value">{{ userInfo?.realName || '张伟' }}</span>
            </div>
            <div class="info-item">
              <span class="label">职称</span>
              <span class="value">{{ userInfo?.title || '副教授' }}</span>
            </div>
            <div class="info-item">
              <span class="label">手机号</span>
              <span class="value">{{ profileForm.phone || '13800138001' }}</span>
            </div>
            <div class="info-item">
              <span class="label">邮箱</span>
              <span class="value">{{ profileForm.email || 'zhangwei@edu.cn' }}</span>
            </div>
            <div class="info-item">
              <span class="label">所属院系</span>
              <span class="value">{{ userInfo?.department || '软件学院' }}</span>
            </div>
            <div class="info-item">
              <span class="label">授课科目</span>
              <span class="value">{{ userInfo?.subjects || 'Java、数据结构' }}</span>
            </div>
          </div>
        </div>

        <!-- 右侧：修改个人信息 -->
        <div class="edit-info-card">
          <h3 class="card-title">修改个人信息</h3>
          <el-form :model="profileForm" label-width="80px" class="edit-form">
            <el-form-item label="姓名">
              <el-input v-model="profileForm.realName" placeholder="请输入姓名" />
            </el-form-item>
            <el-form-item label="职称">
              <el-input v-model="profileForm.title" placeholder="请输入职称" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="头像">
              <div class="upload-area">
                <el-upload
                  class="avatar-upload-btn"
                  action="#"
                  :show-file-list="false"
                  :before-upload="handleUploadAvatar"
                  accept="image/*"
                >
                  <el-button type="primary" size="small">点击上传</el-button>
                </el-upload>
                <span class="upload-tip">支持JPG、PNG格式，大小不超过10MB</span>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleUpdateProfile" class="save-btn">保存修改</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <!-- 下半部分 -->
      <div class="bottom-section">
        <!-- 左侧：修改密码 -->
        <div class="password-card">
          <h3 class="card-title">修改密码</h3>
          <el-form :model="passwordForm" label-width="100px" class="password-form">
            <el-form-item label="当前密码">
              <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入当前密码" show-password />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码（6-20位字母数字组合）" show-password @input="checkPasswordStrength" />
            </el-form-item>
            <el-form-item label="确认新密码">
              <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
            </el-form-item>
            <el-form-item>
              <div v-if="passwordStrength" class="password-strength" :class="passwordStrength.class">
                {{ passwordStrength.text }}
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleChangePassword" class="save-btn">确认修改</el-button>
            </el-form-item>
            <div class="security-tip">
              安全提示：建议定期更换密码，不要使用与其他网站相同的密码
            </div>
          </el-form>
        </div>

        <!-- 右侧：安全设置 -->
        <div class="security-card">
          <h3 class="card-title">安全设置</h3>
          <div class="security-list">
            <div class="security-item">
              <div class="security-info">
                <div class="security-title">登录二次验证</div>
                <div class="security-desc">开启后登录需要短信验证码</div>
              </div>
              <div class="security-status enabled">已开启</div>
            </div>
            <div class="security-item">
              <div class="security-info">
                <div class="security-title">异地登录提醒</div>
                <div class="security-desc">检测到异地登录时发送邮件提醒</div>
              </div>
              <div class="security-status enabled">已开启</div>
            </div>
            <div class="security-item">
              <div class="security-info">
                <div class="security-title">操作日志记录</div>
                <div class="security-desc">所有管理操作都会被记录</div>
              </div>
              <div class="security-status enabled">已开启</div>
            </div>
            <div class="security-item">
              <div class="security-info">
                <div class="security-title">会话超时时间</div>
                <div class="security-desc">无操作30分钟后自动退出</div>
              </div>
              <div class="security-status timeout">30分钟</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

// 头像颜色
const avatarColor = '#1890ff'

const profileForm = reactive({
  realName: '',
  phone: '',
  email: '',
  avatar: '',
  title: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 密码强度
const passwordStrength = ref(null)

// 检查密码强度
const checkPasswordStrength = () => {
  const password = passwordForm.newPassword
  if (!password) {
    passwordStrength.value = null
    return
  }
  
  let strength = 0
  if (password.length >= 6) strength++
  if (password.length >= 10) strength++
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) strength++
  if (/\d/.test(password)) strength++
  if (/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)) strength++
  
  if (strength <= 2) {
    passwordStrength.value = { class: 'weak', text: '密码强度：弱' }
  } else if (strength <= 4) {
    passwordStrength.value = { class: 'medium', text: '密码强度：中（建议包含大小写字母、数字、特殊符号）' }
  } else {
    passwordStrength.value = { class: 'strong', text: '密码强度：强（包含大小写字母、数字、特殊符号）' }
  }
}

// 加载用户信息
const loadProfile = async () => {
  try {
    const res = await request.get('/auth/info')
    if (res.data) {
      profileForm.realName = res.data.realName || ''
      profileForm.phone = res.data.phone || ''
      profileForm.email = res.data.email || ''
      profileForm.avatar = res.data.avatar || ''
      profileForm.title = res.data.title || ''
      
      // 同步更新全局 store，确保其他页面也能获取到最新数据
      userStore.setUserInfo({
        ...userStore.userInfo,
        realName: res.data.realName,
        phone: res.data.phone,
        email: res.data.email,
        avatar: res.data.avatar,
        title: res.data.title
      })
    }
  } catch (error) {
    console.error('加载用户信息失败:', error)
  }
}

// 更新个人信息
const handleUpdateProfile = async () => {
  try {
    await request.put('/auth/profile', profileForm)
    ElMessage.success('更新成功')
    userStore.setUserInfo({
      ...userInfo.value,
      realName: profileForm.realName,
      phone: profileForm.phone,
      email: profileForm.email,
      title: profileForm.title
    })
  } catch (error) {
    ElMessage.error('更新失败')
  }
}

// 上传头像
const handleUploadAvatar = async (file) => {
  const formData = new FormData()
  // before-upload钩子中的file参数本身就是File对象
  formData.append('file', file)
  
  try {
    const res = await request.post('/upload/avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    
    if (res.data && res.data.url) {
      // 更新头像URL到后端
      await request.put('/auth/avatar', { avatar: res.data.url })
      profileForm.avatar = res.data.url
      
      // 更新store中的用户信息
      userStore.setUserInfo({
        ...userInfo.value,
        avatar: res.data.url
      })
      
      ElMessage.success('头像上传成功')
    }
  } catch (error) {
    console.error('头像上传失败:', error)
    const errorMsg = error.response?.data?.message || error.message || '头像上传失败'
    ElMessage.error(errorMsg)
  }
  
  return false // 阻止默认上传行为
}

// 修改密码
const handleChangePassword = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    ElMessage.warning('请填写完整密码信息')
    return
  }
  
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次密码输入不一致')
    return
  }
  
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('密码长度不能少于6位')
    return
  }
  
  try {
    await request.put('/auth/password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordStrength.value = null
    setTimeout(() => {
      userStore.logout()
      window.location.href = '/login'
    }, 1500)
  } catch (error) {
    ElMessage.error('密码修改失败')
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped lang="scss">
.profile-page {
  padding: 16px 20px;
  min-height: calc(100vh - 56px);
  background: #f0f2f5;
  
  .breadcrumb {
    font-size: 14px;
    color: #999;
    margin-bottom: 16px;
  }
  
  .content-wrapper {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  
  // 上半部分
  .top-section {
    display: flex;
    gap: 16px;
    
    // 左侧用户信息卡片
    .user-info-card {
      flex: 1;
      background: #fff;
      border-radius: 8px;
      padding: 24px;
      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
      
      .avatar-wrapper {
        text-align: center;
        margin-bottom: 24px;
        padding-bottom: 24px;
        border-bottom: 1px solid #f0f0f0;
        
        .avatar-uploader {
          display: inline-block;
          position: relative;
          cursor: pointer;
          
          &:hover .avatar-overlay {
            opacity: 1;
          }
          
          .avatar-image {
            width: 100px;
            height: 100px;
            border-radius: 50%;
            margin: 0 auto 16px;
            background-size: cover;
            background-position: center;
            background-repeat: no-repeat;
            border: 3px solid #fff;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
          }
          
          .avatar-circle {
            width: 100px;
            height: 100px;
            border-radius: 50%;
            margin: 0 auto 16px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 40px;
            color: #fff;
            font-weight: bold;
            border: 3px solid #fff;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
          }
          
          .avatar-overlay {
            position: absolute;
            top: 0;
            left: 50%;
            transform: translateX(-50%);
            width: 100px;
            height: 100px;
            border-radius: 50%;
            background: rgba(0, 0, 0, 0.5);
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            opacity: 0;
            transition: opacity 0.3s;
            color: #fff;
            
            .el-icon {
              font-size: 24px;
              margin-bottom: 4px;
            }
            
            span {
              font-size: 12px;
            }
          }
        }
        
        .user-name {
          font-size: 20px;
          font-weight: 600;
          color: #333;
          margin: 0 0 8px 0;
        }
        
        .user-email {
          font-size: 14px;
          color: #999;
        }
      }
      
      .info-list {
        .info-item {
          display: flex;
          justify-content: space-between;
          padding: 12px 0;
          border-bottom: 1px solid #f5f5f5;
          
          &:last-child {
            border-bottom: none;
          }
          
          .label {
            color: #999;
            font-size: 14px;
          }
          
          .value {
            color: #333;
            font-size: 14px;
            font-weight: 500;
          }
        }
      }
    }
    
    // 右侧编辑信息卡片
    .edit-info-card {
      flex: 1.5;
      background: #fff;
      border-radius: 8px;
      padding: 24px;
      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
      
      .card-title {
        font-size: 18px;
        font-weight: 600;
        color: #333;
        margin: 0 0 24px 0;
      }
      
      .edit-form {
        :deep(.el-form-item) {
          margin-bottom: 20px;
        }
        
        .upload-area {
          display: flex;
          align-items: center;
          gap: 12px;
          
          .avatar-upload-btn {
            display: inline-block;
          }
          
          .upload-tip {
            font-size: 12px;
            color: #999;
          }
        }
        
        .save-btn {
          width: 160px;
        }
      }
    }
  }
  
  // 下半部分
  .bottom-section {
    display: flex;
    gap: 16px;
    
    // 左侧修改密码卡片
    .password-card {
      flex: 1.5;
      background: #fff;
      border-radius: 8px;
      padding: 24px;
      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
      
      .card-title {
        font-size: 18px;
        font-weight: 600;
        color: #333;
        margin: 0 0 24px 0;
      }
      
      .password-form {
        :deep(.el-form-item) {
          margin-bottom: 20px;
        }
        
        .password-strength {
          padding: 8px 12px;
          border-radius: 4px;
          font-size: 12px;
          font-weight: 500;
          
          &.weak {
            background-color: #fff1f0;
            color: #f5222d;
            border: 1px solid #ffa39e;
          }
          
          &.medium {
            background-color: #fffbe6;
            color: #faad14;
            border: 1px solid #ffe58f;
          }
          
          &.strong {
            background-color: #f6ffed;
            color: #52c41a;
            border: 1px solid #b7eb8f;
          }
        }
        
        .save-btn {
          width: 160px;
        }
        
        .security-tip {
          margin-top: 16px;
          font-size: 12px;
          color: #999;
          padding: 8px 12px;
          background: #fafafa;
          border-radius: 4px;
        }
      }
    }
    
    // 右侧安全设置卡片
    .security-card {
      flex: 1;
      background: #fff;
      border-radius: 8px;
      padding: 24px;
      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
      
      .card-title {
        font-size: 18px;
        font-weight: 600;
        color: #333;
        margin: 0 0 24px 0;
      }
      
      .security-list {
        .security-item {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 16px 0;
          border-bottom: 1px solid #f5f5f5;
          
          &:last-child {
            border-bottom: none;
          }
          
          .security-info {
            flex: 1;
            
            .security-title {
              color: #333;
              font-size: 14px;
              font-weight: 500;
              margin-bottom: 4px;
            }
            
            .security-desc {
              color: #999;
              font-size: 12px;
            }
          }
          
          .security-status {
            padding: 4px 16px;
            border-radius: 4px;
            font-size: 12px;
            font-weight: 500;
            
            &.enabled {
              background: #f6ffed;
              color: #52c41a;
            }
            
            &.timeout {
              background: #e6f7ff;
              color: #1890ff;
            }
          }
        }
      }
    }
  }
}
</style>
