<template>
  <div class="profile-page">
    <!-- 顶部用户信息卡片 -->
    <div class="user-info-card">
      <div class="avatar-section">
        <div class="avatar-circle" :style="{ backgroundColor: avatarColor }">
          {{ userInfo?.realName?.charAt(0) || '张' }}
        </div>
        <h3 class="user-name">{{ userInfo?.realName || '张三' }}</h3>
        <p class="user-class">{{ userInfo?.className || '软件工程2301班' }}</p>
      </div>
      <div class="info-table">
        <div class="info-row">
          <span class="label">学号</span>
          <span class="value">{{ userInfo?.username || '20230015' }}</span>
        </div>
        <div class="info-row">
          <span class="label">姓名</span>
          <span class="value">{{ userInfo?.realName || '张三' }}</span>
        </div>
        <div class="info-row">
          <span class="label">性别</span>
          <span class="value">{{ userInfo?.gender || '男' }}</span>
        </div>
        <div class="info-row">
          <span class="label">班级</span>
          <span class="value">{{ userInfo?.className || '软件工程2301' }}</span>
        </div>
        <div class="info-row">
          <span class="label">专业</span>
          <span class="value">{{ userInfo?.major || '软件工程' }}</span>
        </div>
        <div class="info-row">
          <span class="label">院系</span>
          <span class="value">{{ userInfo?.department || '软件学院' }}</span>
        </div>
        <div class="info-row">
          <span class="label">手机号</span>
          <span class="value">{{ userInfo?.phone || '13800138015' }}</span>
        </div>
        <div class="info-row">
          <span class="label">邮箱</span>
          <span class="value">{{ userInfo?.email || 'zhangsan@edu.cn' }}</span>
        </div>
      </div>
    </div>

    <!-- 修改个人信息和密码 -->
    <div class="content-grid">
      <!-- 修改个人信息 -->
      <el-card class="form-card">
        <template #header>
          <span class="card-title">修改个人信息</span>
        </template>
        <el-form :model="profileForm" label-width="80px">
          <el-form-item label="姓名">
            <el-input v-model="profileForm.realName" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
          </el-form-item>
          <el-form-item label="头像">
            <el-upload
              class="avatar-uploader"
              action="#"
              :show-file-list="false"
              :auto-upload="false"
            >
              <el-button size="small">点击上传</el-button>
            </el-upload>
          </el-form-item>
          <el-form-item>
            <el-button type="success" @click="handleUpdateProfile">保存修改</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 修改密码 -->
      <el-card class="form-card">
        <template #header>
          <span class="card-title">修改密码</span>
        </template>
        <el-form :model="passwordForm" label-width="100px">
          <el-form-item label="当前密码">
            <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入当前密码" show-password />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
          </el-form-item>
          <el-form-item label="确认新密码">
            <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="success" @click="handleChangePassword">确认修改</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 本学期学习统计 -->
      <el-card class="stats-card">
        <template #header>
          <span class="card-title">本学期学习统计</span>
        </template>
        <div class="stats-list">
          <div class="stat-item">
            <div class="stat-color" style="background-color: #1890ff;"></div>
            <span class="stat-label">参加考试</span>
            <span class="stat-value" style="color: #1890ff;">{{ stats.examCount || 6 }} 场</span>
          </div>
          <div class="stat-item">
            <div class="stat-color" style="background-color: #52c41a;"></div>
            <span class="stat-label">平均成绩</span>
            <span class="stat-value" style="color: #52c41a;">{{ stats.avgScore || 83.5 }} 分</span>
          </div>
          <div class="stat-item">
            <div class="stat-color" style="background-color: #faad14;"></div>
            <span class="stat-label">最高成绩</span>
            <span class="stat-value" style="color: #faad14;">{{ stats.maxScore || 92 }} 分</span>
          </div>
          <div class="stat-item">
            <div class="stat-color" style="background-color: #13c2c2;"></div>
            <span class="stat-label">及格率</span>
            <span class="stat-value" style="color: #13c2c2;">{{ stats.passRate || 100 }}%</span>
          </div>
          <div class="stat-item">
            <div class="stat-color" style="background-color: #f5222d;"></div>
            <span class="stat-label">错题收录</span>
            <span class="stat-value" style="color: #f5222d;">{{ stats.wrongCount || 28 }} 道</span>
          </div>
          <div class="stat-item">
            <div class="stat-color" style="background-color: #722ed1;"></div>
            <span class="stat-label">知识点薄弱</span>
            <span class="stat-value" style="color: #722ed1;">{{ stats.weakPoints || 'IO流、多线程' }}</span>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

// 头像颜色
const avatarColor = '#52c41a'

const profileForm = reactive({
  realName: '',
  phone: '',
  email: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 学习统计数据
const stats = reactive({
  examCount: 0,
  avgScore: 0,
  maxScore: 0,
  passRate: 0,
  wrongCount: 0,
  weakPoints: ''
})

// 加载用户信息
const loadProfile = async () => {
  try {
    const res = await request.get('/auth/info')
    if (res.data) {
      profileForm.realName = res.data.realName || ''
      profileForm.phone = res.data.phone || ''
      profileForm.email = res.data.email || ''
    }
  } catch (error) {
    console.error('加载用户信息失败:', error)
  }
}

// 加载学习统计数据
const loadStudentStats = async () => {
  try {
    const res = await request.get('/student/profile/stats')
    if (res.data) {
      stats.examCount = res.data.examCount || 0
      stats.avgScore = res.data.avgScore || 0
      stats.maxScore = res.data.maxScore || 0
      stats.passRate = res.data.passRate || 0
      stats.wrongCount = res.data.wrongCount || 0
      stats.weakPoints = res.data.weakPoints || ''
    }
  } catch (error) {
    console.error('加载学习统计数据失败:', error)
  }
}

// 更新个人信息
const handleUpdateProfile = async () => {
  try {
    await request.put('/auth/profile', profileForm)
    ElMessage.success('更新成功')
    // 更新 store 中的用户信息
    userStore.setUserInfo({
      ...userInfo.value,
      realName: profileForm.realName,
      phone: profileForm.phone,
      email: profileForm.email
    })
  } catch (error) {
    ElMessage.error('更新失败')
  }
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
  
  try {
    await request.put('/auth/password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    // 清空密码表单
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    // 退出登录
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
  loadStudentStats()
})
</script>

<style scoped lang="scss">
.profile-page {
  padding: 16px 20px;
  height: calc(100vh - 56px);
  box-sizing: border-box;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

// 顶部用户信息卡片
.user-info-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 20px 24px;
  display: flex;
  gap: 32px;
  align-items: center;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  flex-shrink: 0;
  
  .avatar-section {
    text-align: center;
    min-width: 140px;
    
    .avatar-circle {
      width: 80px;
      height: 80px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto 12px;
      font-size: 32px;
      color: #fff;
      font-weight: bold;
      background: rgba(255, 255, 255, 0.2);
      backdrop-filter: blur(10px);
      border: 3px solid rgba(255, 255, 255, 0.3);
    }
    
    .user-name {
      margin: 0 0 6px 0;
      font-size: 18px;
      font-weight: 600;
      color: #fff;
    }
    
    .user-class {
      margin: 0;
      color: rgba(255, 255, 255, 0.85);
      font-size: 13px;
    }
  }
  
  .info-table {
    flex: 1;
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 12px 24px;
    
    .info-row {
      display: flex;
      align-items: center;
      padding: 6px 0;
      
      .label {
        color: rgba(255, 255, 255, 0.8);
        min-width: 70px;
        font-size: 13px;
      }
      
      .value {
        color: #fff;
        font-weight: 500;
        font-size: 13px;
      }
    }
  }
}

// 内容网格布局
.content-grid {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  overflow: hidden;
  
  .form-card {
    background: #fff;
    border-radius: 10px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    overflow: hidden;
    display: flex;
    flex-direction: column;
    
    :deep(.el-card__header) {
      padding: 14px 20px;
      border-bottom: 2px solid #f0f0f0;
      background: linear-gradient(to right, #fafafa, #fff);
    }
    
    :deep(.el-card__body) {
      padding: 16px 20px;
      flex: 1;
      overflow-y: auto;
    }
    
    .card-title {
      font-size: 15px;
      font-weight: 600;
      color: #333;
      display: flex;
      align-items: center;
      gap: 8px;
      
      &::before {
        content: '';
        width: 4px;
        height: 16px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border-radius: 2px;
      }
    }
    
    :deep(.el-form-item) {
      margin-bottom: 14px;
      
      &:last-child {
        margin-bottom: 0;
      }
    }
    
    :deep(.el-input__wrapper) {
      border-radius: 6px;
    }
  }
  
  .stats-card {
    grid-column: 2;
    background: #fff;
    border-radius: 10px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    overflow: hidden;
    display: flex;
    flex-direction: column;
    
    :deep(.el-card__header) {
      padding: 14px 20px;
      border-bottom: 2px solid #f0f0f0;
      background: linear-gradient(to right, #fafafa, #fff);
    }
    
    :deep(.el-card__body) {
      padding: 16px 20px;
      flex: 1;
      overflow-y: auto;
    }
    
    .card-title {
      font-size: 15px;
      font-weight: 600;
      color: #333;
      display: flex;
      align-items: center;
      gap: 8px;
      
      &::before {
        content: '';
        width: 4px;
        height: 16px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border-radius: 2px;
      }
    }
    
    .stats-list {
      .stat-item {
        display: flex;
        align-items: center;
        padding: 14px 16px;
        margin-bottom: 10px;
        background: linear-gradient(to right, #fafafa, #fff);
        border-radius: 8px;
        transition: all 0.3s;
        border-left: 3px solid transparent;
        
        &:hover {
          transform: translateX(4px);
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }
        
        &:last-child {
          margin-bottom: 0;
        }
        
        .stat-color {
          width: 6px;
          height: 36px;
          border-radius: 3px;
          margin-right: 14px;
        }
        
        .stat-label {
          flex: 1;
          color: #666;
          font-size: 14px;
        }
        
        .stat-value {
          font-weight: 600;
          font-size: 16px;
        }
      }
    }
  }
}
</style>
