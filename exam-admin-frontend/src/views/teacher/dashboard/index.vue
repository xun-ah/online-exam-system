<template>
  <div class="teacher-dashboard">
    <!-- 欢迎卡片 -->
    <el-card class="welcome-card" shadow="never">
      <div class="welcome-content">
        <div class="welcome-left">
          <!-- 显示真实头像或默认文字头像 -->
          <div 
            v-if="userInfo?.avatar" 
            class="avatar avatar-image"
            :style="{ backgroundImage: `url(${userInfo.avatar})` }"
          ></div>
          <div v-else class="avatar">{{ userInfo?.realName?.charAt(0) || '张' }}</div>
          <div class="info">
            <h2>欢迎回来，{{ userInfo?.realName || '张老师' }}！</h2>
            <p class="detail">
              <span>工号：{{ userInfo?.username || 'T2024001' }}</span>
              <span>|</span>
              <span>所属院系：{{ userInfo?.department || '加载中...' }}</span>
              <span v-if="stats.courseCount !== undefined">|</span>
              <span v-if="stats.courseCount !== undefined">本学期授课：{{ stats.courseCount }} 门</span>
            </p>
          </div>
        </div>
        <div class="welcome-right">
          <el-button type="primary" @click="$router.push('/teacher/question-bank')">+ 新增题目</el-button>
          <el-button type="success" @click="$router.push('/teacher/exam')">发布考试</el-button>
        </div>
      </div>
    </el-card>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <el-card class="stat-card blue" shadow="never">
        <div class="stat-value">{{ stats.questionCount || 0 }}</div>
        <div class="stat-label">我的题库</div>
        <div class="stat-sub">本月新增 {{ stats.monthlyNewQuestions || 0 }} 道</div>
      </el-card>
      <el-card class="stat-card green" shadow="never">
        <div class="stat-value">{{ stats.paperCount || 0 }}</div>
        <div class="stat-label">已组试卷</div>
        <div class="stat-sub">本月新增 {{ stats.monthlyNewPapers || 0 }} 套</div>
      </el-card>
      <el-card class="stat-card orange" shadow="never">
        <div class="stat-value">{{ stats.examCount || 0 }}</div>
        <div class="stat-label">已发布考试</div>
        <div class="stat-sub">进行中 {{ stats.ongoingExamCount || 0 }} 场</div>
      </el-card>
      <el-card class="stat-card red" shadow="never">
        <div class="stat-value">{{ stats.pendingGradingCount || 0 }}</div>
        <div class="stat-label">待阅卷</div>
        <div class="stat-sub">紧急 {{ stats.urgentGradingCount || 0 }} 份</div>
      </el-card>
    </div>

    <!-- 待办事项和近期考试 -->
    <div class="content-row">
      <!-- 待办事项 -->
      <el-card class="todo-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>待办事项</span>
          </div>
        </template>
        <div v-if="todoList.length === 0" class="empty-tip">暂无待办事项</div>
        <div v-for="todo in todoList" :key="todo.id" class="todo-item" :class="todo.priority">
          <div class="todo-info">
            <div class="todo-title">{{ todo.title }}</div>
            <div class="todo-desc">{{ todo.description }}</div>
          </div>
          <div class="todo-right">
            <span class="time">{{ formatTime(todo.createTime) }}</span>
            <el-tag :type="getPriorityTagType(todo.priority)" size="small">{{ getPriorityText(todo.priority) }}</el-tag>
          </div>
        </div>
      </el-card>

      <!-- 近期考试状态 -->
      <el-card class="exam-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>近期考试状态</span>
            <el-link type="primary">查看全部 ></el-link>
          </div>
        </template>
        <div v-if="recentExams.length === 0" class="empty-tip">暂无考试记录</div>
        <div v-for="exam in recentExams" :key="exam.id" class="exam-item">
          <div class="exam-left">
            <div class="dot" :class="exam.status === 1 ? 'green' : 'gray'"></div>
            <div>
              <div class="exam-title">{{ exam.examName }}</div>
              <div class="exam-desc">考试时间：{{ formatDateTime(exam.startTime) }} | 参考人数：{{ exam.participantCount }}人</div>
            </div>
          </div>
          <div class="exam-right">
            <el-tag :type="exam.status === 1 ? 'success' : 'info'">{{ exam.status === 1 ? '进行中' : '已结束' }}</el-tag>
            <el-button :type="exam.status === 1 ? 'primary' : ''" size="small" style="margin-top: 8px;">{{ exam.status === 1 ? '监控' : '分析' }}</el-button>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 本学期教学数据概览 -->
    <el-card class="data-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <span class="main-title">本学期教学数据概览</span>
            <span class="sub-title">各科目平均分对比</span>
          </div>
        </div>
      </template>
      <div class="data-content">
        <!-- 柱状图区域 -->
        <div class="chart-section">
          <div class="bar-chart">
            <div class="bar-item" v-for="item in subjectData" :key="item.name">
              <div class="bar-value" :style="{ height: item.percent + '%', backgroundColor: item.color }">{{ item.score }}</div>
              <div class="bar-label">{{ item.name }}</div>
            </div>
          </div>
          <div class="legend">
            <span class="legend-item">我的班次</span>
            <span class="legend-item gray">年级平均</span>
          </div>
        </div>

        <!-- 饼图区域 -->
        <div class="pie-section">
          <div class="section-title">题型使用分布</div>
          <div class="pie-chart">
            <div class="pie-segment" v-for="item in questionTypeData" :key="item.name" 
                 :style="{ backgroundColor: item.color, transform: `rotate(${item.rotate}deg)` }">
            </div>
          </div>
          <div class="pie-legend">
            <div class="pie-legend-item" v-for="item in questionTypeData" :key="item.name">
              <span class="dot" :style="{ backgroundColor: item.color }"></span>
              <span>{{ item.name }} {{ item.percent }}%</span>
            </div>
          </div>
        </div>

        <!-- 统计信息 -->
        <div class="stats-section">
          <div class="stat-row">
            <span>本学期授课</span>
            <strong>{{ stats.courseCount || 0 }} 门</strong>
          </div>
          <div class="stat-row">
            <span>覆盖学生</span>
            <strong>{{ stats.studentCount || 0 }} 人</strong>
          </div>
          <div class="stat-row">
            <span>考试场次</span>
            <strong>{{ stats.examSessionCount || 0 }} 场</strong>
          </div>
          <div class="stat-row">
            <span>平均及格率</span>
            <strong>{{ stats.passRate || 0 }}%</strong>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '@/store/user'
import { getTeacherDashboardStats, getTodos, getRecentExams, getTeachingData } from '@/api/teacher/dashboard'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
// 使用 computed 确保 userInfo 始终与 store 保持同步
const userInfo = computed(() => userStore.userInfo || {})

// 获取教师详细信息（包括院系）
// 注意：不再从 dashboard stats 中获取用户信息，避免覆盖个人中心修改的数据
// 用户信息应该只通过 /auth/info API 和个人中心页面来更新
const fetchTeacherInfo = async () => {
  try {
    // 只获取统计数据，不更新用户信息
    const res = await getTeacherDashboardStats()
    if (res.data) {
      // 更新userInfo中的院系信息
      if (res.data.departmentName) {
        userStore.setUserInfo({
          ...userStore.userInfo,
          department: res.data.departmentName
        })
      }
    }
  } catch (error) {
    console.error('获取教师信息失败:', error)
  }
}

const stats = reactive({
  questionCount: 0,
  monthlyNewQuestions: 0,
  paperCount: 0,
  monthlyNewPapers: 0,
  examCount: 0,
  ongoingExamCount: 0,
  pendingGradingCount: 0,
  urgentGradingCount: 0,
  courseCount: 0,
  studentCount: 0,
  examSessionCount: 0,
  passRate: 0
})

// 待办事项数据
const todoList = ref([])

// 近期考试数据
const recentExams = ref([])

// 教学数据
const subjectData = ref([])
const questionTypeData = ref([])

// 获取统计数据
const fetchStats = async () => {
  try {
    const res = await getTeacherDashboardStats()
    Object.assign(stats, res.data)
  } catch (error) {
    console.error('获取统计数据失败:', error)
    ElMessage.error('获取统计数据失败')
  }
}

// 获取待办事项
const fetchTodos = async () => {
  try {
    const res = await getTodos()
    todoList.value = res.data || []
  } catch (error) {
    console.error('获取待办事项失败:', error)
  }
}

// 获取近期考试
const fetchRecentExams = async () => {
  try {
    const res = await getRecentExams()
    recentExams.value = res.data || []
  } catch (error) {
    console.error('获取近期考试失败:', error)
  }
}

// 获取教学数据
const fetchTeachingData = async () => {
  try {
    const res = await getTeachingData()
    if (res.data) {
      subjectData.value = res.data.subjectData || []
      questionTypeData.value = res.data.questionTypeData || []
      
      // 更新教学统计信息
      if (res.data.courseCount !== undefined) stats.courseCount = res.data.courseCount
      if (res.data.studentCount !== undefined) stats.studentCount = res.data.studentCount
      if (res.data.examSessionCount !== undefined) stats.examSessionCount = res.data.examSessionCount
      if (res.data.passRate !== undefined) stats.passRate = res.data.passRate
    }
  } catch (error) {
    console.error('获取教学数据失败:', error)
  }
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const now = new Date()
  const timeDate = new Date(time)
  const diff = now - timeDate
  
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return Math.floor(diff / (60 * 1000)) + '分钟前'
  if (diff < 24 * 60 * 60 * 1000) return Math.floor(diff / (60 * 60 * 1000)) + '小时前'
  if (diff < 7 * 24 * 60 * 60 * 1000) return Math.floor(diff / (24 * 60 * 60 * 1000)) + '天前'
  
  return timeDate.toLocaleDateString()
}

// 格式化日期时间
const formatDateTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

// 获取优先级标签类型
const getPriorityTagType = (priority) => {
  const typeMap = {
    urgent: 'danger',
    warning: 'warning',
    normal: 'primary'
  }
  return typeMap[priority] || 'info'
}

// 获取优先级文本
const getPriorityText = (priority) => {
  const textMap = {
    urgent: '紧急',
    warning: '进行中',
    normal: '待处理'
  }
  return textMap[priority] || '普通'
}

onMounted(() => {
  fetchTeacherInfo() // 获取教师详细信息
  fetchStats()
  fetchTodos()
  fetchRecentExams()
  fetchTeachingData()
})
</script>

<style scoped lang="scss">
.teacher-dashboard {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 56px);
}

.welcome-card {
  margin-bottom: 20px;
  border-radius: 8px;
  
  .welcome-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .welcome-left {
      display: flex;
      align-items: center;
      gap: 20px;
      
      .avatar {
        width: 60px;
        height: 60px;
        border-radius: 50%;
        background: linear-gradient(135deg, #409eff, #66b1ff);
        color: #fff;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 24px;
        font-weight: bold;
        
        &.avatar-image {
          background-size: cover;
          background-position: center;
          background-repeat: no-repeat;
          border: 3px solid #fff;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }
      }
      
      .info {
        h2 {
          margin: 0 0 8px 0;
          font-size: 20px;
          color: #303133;
        }
        
        .detail {
          margin: 0;
          color: #909399;
          font-size: 14px;
          
          span {
            margin: 0 8px;
            
            &:first-child {
              margin-left: 0;
            }
          }
        }
      }
    }
    
    .welcome-right {
      display: flex;
      gap: 12px;
    }
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
  
  .stat-card {
    border-radius: 8px;
    text-align: center;
    padding: 20px;
    position: relative;
    overflow: hidden;
    
    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 4px;
    }
    
    &.blue::before { background: #409eff; }
    &.green::before { background: #67c23a; }
    &.orange::before { background: #e6a23c; }
    &.red::before { background: #f56c6c; }
    
    .stat-value {
      font-size: 32px;
      font-weight: bold;
      margin-bottom: 8px;
      
      &.blue { color: #409eff; }
      &.green { color: #67c23a; }
      &.orange { color: #e6a23c; }
      &.red { color: #f56c6c; }
    }
    
    .stat-label {
      font-size: 14px;
      color: #606266;
      margin-bottom: 4px;
    }
    
    .stat-sub {
      font-size: 12px;
      color: #909399;
    }
  }
}

.content-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: bold;
    font-size: 16px;
  }
}

.todo-card, .exam-card {
  border-radius: 8px;
  
  .empty-tip {
    text-align: center;
    padding: 40px 0;
    color: #909399;
    font-size: 14px;
  }
  
  .todo-item, .exam-item {
    padding: 16px;
    border-radius: 6px;
    margin-bottom: 12px;
    background: #f9f9f9;
    
    &:last-child {
      margin-bottom: 0;
    }
    
    &.urgent {
      border-left: 4px solid #f56c6c;
    }
    
    &.warning {
      border-left: 4px solid #e6a23c;
    }
    
    &.normal {
      border-left: 4px solid #409eff;
    }
  }
  
  .todo-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .todo-info {
      .todo-title {
        font-weight: bold;
        margin-bottom: 4px;
      }
      
      .todo-desc {
        font-size: 12px;
        color: #909399;
      }
    }
    
    .todo-right {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 8px;
      
      .time {
        font-size: 12px;
        color: #909399;
      }
    }
  }
  
  .exam-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .exam-left {
      display: flex;
      align-items: center;
      gap: 12px;
      
      .dot {
        width: 12px;
        height: 12px;
        border-radius: 50%;
        
        &.green { background: #67c23a; }
        &.gray { background: #c0c4cc; }
      }
      
      .exam-title {
        font-weight: bold;
        margin-bottom: 4px;
      }
      
      .exam-desc {
        font-size: 12px;
        color: #909399;
      }
    }
    
    .exam-right {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
    }
  }
}

.data-card {
  border-radius: 8px;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .main-title {
      font-weight: bold;
      font-size: 16px;
    }
    
    .sub-title {
      margin-left: 20px;
      color: #909399;
      font-size: 14px;
    }
  }
  
  .data-content {
    display: grid;
    grid-template-columns: 1fr 1fr 1fr;
    gap: 40px;
    padding: 20px 0;
  }
  
  .chart-section {
    .bar-chart {
      display: flex;
      justify-content: space-around;
      align-items: flex-end;
      height: 150px;
      padding: 20px 0;
      
      .bar-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 8px;
        
        .bar-value {
          width: 30px;
          border-radius: 4px 4px 0 0;
          color: #fff;
          font-size: 12px;
          display: flex;
          align-items: flex-end;
          justify-content: center;
          padding-bottom: 4px;
        }
        
        .bar-label {
          font-size: 12px;
          color: #606266;
        }
      }
    }
    
    .legend {
      display: flex;
      justify-content: center;
      gap: 20px;
      margin-top: 10px;
      
      .legend-item {
        font-size: 12px;
        
        &.gray {
          color: #c0c4cc;
        }
      }
    }
  }
  
  .pie-section {
    text-align: center;
    
    .section-title {
      font-size: 14px;
      color: #606266;
      margin-bottom: 20px;
    }
    
    .pie-chart {
      width: 150px;
      height: 150px;
      border-radius: 50%;
      margin: 0 auto 20px;
      position: relative;
      overflow: hidden;
      background: conic-gradient(
        #409eff 0deg 126deg,
        #67c23a 126deg 198deg,
        #e6a23c 198deg 252deg,
        #f56c6c 252deg 316.8deg,
        #7c3aed 316.8deg 360deg
      );
    }
    
    .pie-legend {
      display: flex;
      flex-direction: column;
      gap: 8px;
      
      .pie-legend-item {
        font-size: 12px;
        color: #606266;
        
        .dot {
          display: inline-block;
          width: 8px;
          height: 8px;
          border-radius: 50%;
          margin-right: 4px;
        }
      }
    }
  }
  
  .stats-section {
    .stat-row {
      display: flex;
      justify-content: space-between;
      padding: 12px 0;
      border-bottom: 1px solid #ebeef5;
      
      &:last-child {
        border-bottom: none;
      }
      
      span {
        color: #909399;
      }
      
      strong {
        color: #303133;
      }
    }
  }
}
</style>
