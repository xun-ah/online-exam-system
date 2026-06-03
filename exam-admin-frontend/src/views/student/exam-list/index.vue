<template>
  <div class="exam-list-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>我的考试</h2>
      <p>查看并参加即将开始的考试</p>
    </div>

    <!-- 标签页 -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="待考考试" name="pending">
        <div class="exam-list">
          <el-card v-for="exam in filteredExamList" :key="exam.id" class="exam-card" :class="getExamStatusClass(exam.status)">
            <div class="exam-header">
              <h3 class="exam-name">{{ exam.examName }}</h3>
              <div class="status-tags">
                <el-tag :type="getStatusType(exam.status)" size="large">
                  {{ getStatusText(exam.status) }}
                </el-tag>
                <el-tag v-if="exam.absentReason" type="danger" size="large" effect="dark">
                  <el-icon><WarningFilled /></el-icon>
                  未参加
                </el-tag>
              </div>
            </div>
            
            <div class="exam-info">
              <div class="info-item">
                <el-icon><Document /></el-icon>
                <span>试卷：{{ exam.paperName || '暂无' }}</span>
              </div>
              <div class="info-item">
                <el-icon><School /></el-icon>
                <span>班级：{{ exam.className || '暂无' }}</span>
              </div>
              <div class="info-item">
                <el-icon><Calendar /></el-icon>
                <span>开始时间：{{ formatDateTime(exam.startTime) }}</span>
              </div>
              <div class="info-item">
                <el-icon><Clock /></el-icon>
                <span>结束时间：{{ formatDateTime(exam.endTime) }}</span>
              </div>
              <div class="info-item">
                <el-icon><Timer /></el-icon>
                <span>考试时长：{{ exam.duration || 0 }} 分钟</span>
              </div>
              <div v-if="exam.absentReason" class="info-item absent-reason">
                <el-icon><WarningFilled /></el-icon>
                <span>{{ exam.absentReason }}</span>
              </div>
            </div>
            
            <div class="exam-actions">
              <el-button 
                v-if="exam.status === 1 && !exam.completed" 
                type="primary" 
                size="large"
                @click="enterExam(exam)"
              >
                <el-icon><VideoPlay /></el-icon>
                进入考场
              </el-button>
              <el-button 
                v-else-if="exam.status === 0 && !exam.completed" 
                type="info" 
                size="large"
                disabled
              >
                <el-icon><Lock /></el-icon>
                未开始
              </el-button>
              <el-button 
                v-else-if="exam.completed"
                type="success" 
                size="large"
                disabled
              >
                <el-icon><CircleCheck /></el-icon>
                已完成
              </el-button>
              <el-button 
                v-else 
                type="info" 
                size="large"
                disabled
              >
                <el-icon><CircleClose /></el-icon>
                已结束
              </el-button>
            </div>
          </el-card>

          <!-- 空状态 -->
          <el-empty v-if="filteredExamList.length === 0" description="暂无待考考试">
            <el-button type="primary" @click="refreshList">刷新</el-button>
          </el-empty>
        </div>
      </el-tab-pane>
      
      <el-tab-pane label="进行中" name="ongoing">
        <div class="exam-list">
          <el-card v-for="exam in filteredExamList" :key="exam.id" class="exam-card" :class="getExamStatusClass(exam.status)">
            <div class="exam-header">
              <h3 class="exam-name">{{ exam.examName }}</h3>
              <div class="status-tags">
                <el-tag :type="getStatusType(exam.status)" size="large">
                  {{ getStatusText(exam.status) }}
                </el-tag>
                <el-tag v-if="exam.absentReason" type="danger" size="large" effect="dark">
                  <el-icon><WarningFilled /></el-icon>
                  未参加
                </el-tag>
              </div>
            </div>
            
            <div class="exam-info">
              <div class="info-item">
                <el-icon><Document /></el-icon>
                <span>试卷：{{ exam.paperName || '暂无' }}</span>
              </div>
              <div class="info-item">
                <el-icon><School /></el-icon>
                <span>班级：{{ exam.className || '暂无' }}</span>
              </div>
              <div class="info-item">
                <el-icon><Calendar /></el-icon>
                <span>开始时间：{{ formatDateTime(exam.startTime) }}</span>
              </div>
              <div class="info-item">
                <el-icon><Clock /></el-icon>
                <span>结束时间：{{ formatDateTime(exam.endTime) }}</span>
              </div>
              <div class="info-item">
                <el-icon><Timer /></el-icon>
                <span>考试时长：{{ exam.duration || 0 }} 分钟</span>
              </div>
              <div v-if="exam.absentReason" class="info-item absent-reason">
                <el-icon><WarningFilled /></el-icon>
                <span>{{ exam.absentReason }}</span>
              </div>
            </div>
            
            <div class="exam-actions">
              <el-button 
                v-if="exam.status === 1 && !exam.completed" 
                type="primary" 
                size="large"
                @click="enterExam(exam)"
              >
                <el-icon><VideoPlay /></el-icon>
                进入考场
              </el-button>
              <el-button 
                v-else-if="exam.status === 0 && !exam.completed" 
                type="info" 
                size="large"
                disabled
              >
                <el-icon><Lock /></el-icon>
                未开始
              </el-button>
              <el-button 
                v-else-if="exam.completed"
                type="success" 
                size="large"
                disabled
              >
                <el-icon><CircleCheck /></el-icon>
                已完成
              </el-button>
              <el-button 
                v-else 
                type="info" 
                size="large"
                disabled
              >
                <el-icon><CircleClose /></el-icon>
                已结束
              </el-button>
            </div>
          </el-card>

          <!-- 空状态 -->
          <el-empty v-if="filteredExamList.length === 0" description="暂无进行中的考试">
            <el-button type="primary" @click="refreshList">刷新</el-button>
          </el-empty>
        </div>
      </el-tab-pane>
      
      <el-tab-pane label="已完成" name="completed">
        <div class="exam-list">
          <el-card v-for="exam in filteredExamList" :key="exam.id" class="exam-card" :class="getExamStatusClass(exam.status)">
            <div class="exam-header">
              <h3 class="exam-name">{{ exam.examName }}</h3>
              <div class="status-tags">
                <el-tag :type="getStatusType(exam.status)" size="large">
                  {{ getStatusText(exam.status) }}
                </el-tag>
                <el-tag v-if="exam.absentReason" type="danger" size="large" effect="dark">
                  <el-icon><WarningFilled /></el-icon>
                  未参加
                </el-tag>
              </div>
            </div>
            
            <div class="exam-info">
              <div class="info-item">
                <el-icon><Document /></el-icon>
                <span>试卷：{{ exam.paperName || '暂无' }}</span>
              </div>
              <div class="info-item">
                <el-icon><School /></el-icon>
                <span>班级：{{ exam.className || '暂无' }}</span>
              </div>
              <div class="info-item">
                <el-icon><Calendar /></el-icon>
                <span>开始时间：{{ formatDateTime(exam.startTime) }}</span>
              </div>
              <div class="info-item">
                <el-icon><Clock /></el-icon>
                <span>结束时间：{{ formatDateTime(exam.endTime) }}</span>
              </div>
              <div class="info-item">
                <el-icon><Timer /></el-icon>
                <span>考试时长：{{ exam.duration || 0 }} 分钟</span>
              </div>
              <div v-if="exam.absentReason" class="info-item absent-reason">
                <el-icon><WarningFilled /></el-icon>
                <span>{{ exam.absentReason }}</span>
              </div>
            </div>
            
            <div class="exam-actions">
              <el-button 
                v-if="exam.status === 1 && !exam.completed" 
                type="primary" 
                size="large"
                @click="enterExam(exam)"
              >
                <el-icon><VideoPlay /></el-icon>
                进入考场
              </el-button>
              <el-button 
                v-else-if="exam.status === 0 && !exam.completed" 
                type="info" 
                size="large"
                disabled
              >
                <el-icon><Lock /></el-icon>
                未开始
              </el-button>
              <el-button 
                v-else-if="exam.completed"
                type="success" 
                size="large"
                disabled
              >
                <el-icon><CircleCheck /></el-icon>
                已完成
              </el-button>
              <el-button 
                v-else 
                type="info" 
                size="large"
                disabled
              >
                <el-icon><CircleClose /></el-icon>
                已结束
              </el-button>
            </div>
          </el-card>

          <!-- 空状态 -->
          <el-empty v-if="filteredExamList.length === 0" description="暂无已完成的考试">
            <el-button type="primary" @click="refreshList">刷新</el-button>
          </el-empty>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Document, School, Calendar, Clock, Timer, 
  VideoPlay, Lock, CircleClose, WarningFilled, CircleCheck
} from '@element-plus/icons-vue'
import { getPendingExamList } from '@/api/student'

const router = useRouter()
const activeTab = ref('pending')
const examList = ref([])

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '暂无'
  const date = new Date(dateTime)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    0: '未开始',
    1: '进行中',
    2: '已结束'
  }
  return statusMap[status] || '未知'
}

// 获取状态类型
const getStatusType = (status) => {
  const typeMap = {
    0: 'info',
    1: 'success',
    2: 'danger'
  }
  return typeMap[status] || 'info'
}

// 根据当前标签页过滤考试列表
const filteredExamList = computed(() => {
  if (activeTab.value === 'pending') {
    return examList.value.filter(exam => exam.status === 0 && !exam.completed)
  } else if (activeTab.value === 'ongoing') {
    return examList.value.filter(exam => exam.status === 1 && !exam.completed)
  } else if (activeTab.value === 'completed') {
    return examList.value.filter(exam => exam.status === 2 || exam.completed)
  }
  return []
})

// 获取考试状态样式类
const getExamStatusClass = (status) => {
  if (status === 1) return 'exam-ongoing'
  if (status === 0) return 'exam-pending'
  return 'exam-ended'
}

// 加载考试列表
const loadExamList = async () => {
  try {
    // 获取全部数据，不分页
    const res = await getPendingExamList({ pageNum: 1, pageSize: 1000 })
    if (res.code === 200 && res.data) {
      // 支持分页数据格式
      if (res.data.records) {
        examList.value = res.data.records
      } else if (Array.isArray(res.data)) {
        examList.value = res.data
      }
    }
  } catch (error) {
    console.error('加载考试列表失败:', error)
    ElMessage.error('加载考试列表失败')
  }
}

// 切换标签页
const handleTabChange = () => {
  // 标签页切换时不需要重新加载数据，因为数据已经全部加载
}

// 刷新列表
const refreshList = () => {
  loadExamList()
  ElMessage.success('刷新成功')
}

// 进入考场
const enterExam = (exam) => {
  router.push({
    name: 'OnlineExam',
    query: { examId: exam.id }
  })
}

onMounted(() => {
  loadExamList()
})
</script>

<style scoped lang="scss">
.exam-list-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;
  
  h2 {
    margin: 0 0 8px 0;
    font-size: 24px;
    color: #303133;
  }
  
  p {
    margin: 0;
    color: #909399;
    font-size: 14px;
  }
}

.exam-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.exam-card {
  border-left: 4px solid #409EFF;
  transition: all 0.3s;
  
  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    transform: translateY(-2px);
  }
  
  &.exam-ongoing {
    border-left-color: #67C23A;
  }
  
  &.exam-pending {
    border-left-color: #409EFF;
  }
  
  &.exam-ended {
    border-left-color: #909399;
  }
  
  .exam-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    
    .exam-name {
      margin: 0;
      font-size: 18px;
      color: #303133;
      flex: 1;
    }
    
    .status-tags {
      display: flex;
      gap: 8px;
      align-items: center;
    }
  }
  
  .exam-info {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    margin-bottom: 16px;
    
    .info-item {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #606266;
      font-size: 14px;
      
      .el-icon {
        color: #409EFF;
        font-size: 16px;
      }
      
      &.absent-reason {
        grid-column: 1 / -1;
        color: #F56C6C;
        font-weight: 500;
        background-color: #FEF0F0;
        padding: 8px 12px;
        border-radius: 4px;
        border-left: 3px solid #F56C6C;
        
        .el-icon {
          color: #F56C6C;
        }
      }
    }
  }
  
  .exam-actions {
    display: flex;
    justify-content: flex-end;
    padding-top: 16px;
    border-top: 1px solid #EBEEF5;
  }
}

:deep(.el-card__body) {
  padding: 20px;
}
</style>
