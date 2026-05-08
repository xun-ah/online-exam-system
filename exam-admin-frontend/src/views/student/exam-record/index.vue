<template>
  <div class="exam-record-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>历史考试</h2>
      <p>查看已完成的考试记录和成绩</p>
    </div>

    <!-- 考试记录列表 -->
    <div class="record-list">
      <el-card v-for="record in recordList" :key="record.id" class="record-card">
        <div class="record-header">
          <h3 class="exam-name">{{ record.examName }}</h3>
          <el-tag :type="getScoreTagType(record.score)" size="large">
            {{ record.score !== null ? record.score + '分' : '未评分' }}
          </el-tag>
        </div>
        
        <div class="record-info">
          <div class="info-item">
            <el-icon><Document /></el-icon>
            <span>试卷：{{ record.paperName || '暂无' }}</span>
          </div>
          <div class="info-item">
            <el-icon><School /></el-icon>
            <span>班级：{{ record.className || '暂无' }}</span>
          </div>
          <div class="info-item">
            <el-icon><Calendar /></el-icon>
            <span>开始时间：{{ formatDateTime(record.startTime) }}</span>
          </div>
          <div class="info-item">
            <el-icon><Clock /></el-icon>
            <span>结束时间：{{ formatDateTime(record.endTime) }}</span>
          </div>
          <div class="info-item">
            <el-icon><Timer /></el-icon>
            <span>考试时长：{{ record.duration || 0 }} 分钟</span>
          </div>
          <div class="info-item" v-if="record.submitTime">
            <el-icon><CircleCheck /></el-icon>
            <span>提交时间：{{ formatDateTime(record.submitTime) }}</span>
          </div>
        </div>
        
        <div class="record-actions">
          <el-button 
            type="primary" 
            size="small"
            @click="viewDetail(record)"
          >
            <el-icon><View /></el-icon>
            查看详情
          </el-button>
        </div>
      </el-card>

      <!-- 空状态 -->
      <el-empty v-if="recordList.length === 0" description="暂无考试记录">
        <el-button type="primary" @click="refreshList">刷新</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Document, School, Calendar, Clock, Timer, 
  CircleCheck, View 
} from '@element-plus/icons-vue'
import { getExamRecordList } from '@/api/student'

const router = useRouter()
const recordList = ref([])

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

// 获取分数标签类型
const getScoreTagType = (score) => {
  if (score === null || score === undefined) return 'info'
  if (score >= 90) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}

// 加载考试记录
const loadRecordList = async () => {
  try {
    const res = await getExamRecordList()
    if (res.code === 200 && res.data) {
      recordList.value = res.data
    }
  } catch (error) {
    console.error('加载考试记录失败:', error)
    ElMessage.error('加载考试记录失败')
  }
}

// 刷新列表
const refreshList = () => {
  loadRecordList()
  ElMessage.success('刷新成功')
}

// 查看详情
const viewDetail = (record) => {
  router.push({
    name: 'ScoreDetail',
    query: { recordId: record.id }
  })
}

onMounted(() => {
  loadRecordList()
})
</script>

<style scoped lang="scss">
.exam-record-page {
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

.record-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.record-card {
  border-left: 4px solid #409EFF;
  transition: all 0.3s;
  
  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    transform: translateY(-2px);
  }
  
  .record-header {
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
  }
  
  .record-info {
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
    }
  }
  
  .record-actions {
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
