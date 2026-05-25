<template>
  <div class="score-detail-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <el-button @click="goBack" :icon="ArrowLeft">返回列表</el-button>
      <h2>{{ examDetail.examName || '考试详情' }}</h2>
    </div>

    <!-- 考试信息 -->
    <el-card class="info-card" shadow="never" v-loading="loading">
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="info-item">
            <span class="label">得分：</span>
            <el-tag :type="getScoreType(examDetail.score)" size="large">
              {{ examDetail.score || 0 }}分
            </el-tag>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="info-item">
            <span class="label">考试时长：</span>
            <span>{{ examDetail.duration || 0 }}分钟</span>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="info-item">
            <span class="label">提交时间：</span>
            <span>{{ formatDateTime(examDetail.submitTime) }}</span>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="info-item">
            <span class="label">状态：</span>
            <el-tag :type="examDetail.score >= 60 ? 'success' : 'danger'">
              {{ examDetail.score >= 60 ? '及格' : '不及格' }}
            </el-tag>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 题目详情列表 -->
    <el-card class="questions-card" shadow="never">
      <template #header>
        <span>答题详情</span>
      </template>
      
      <div class="question-list">
        <el-card 
          v-for="(question, index) in questionList" 
          :key="question.questionId"
          class="question-item"
        >
          <div class="question-header">
            <el-tag :type="getTypeTagType(question.type)" size="large">
              {{ getTypeText(question.type) }}
            </el-tag>
            <span class="question-index">第{{ index + 1 }}题</span>
            <el-tag 
              :type="question.isCorrect ? 'success' : 'danger'" 
              size="large"
            >
              {{ question.isCorrect ? '正确' : '错误' }}
            </el-tag>
          </div>

          <div class="question-content">
            <div class="content-label">题目：</div>
            <div class="content-text">{{ question.content }}</div>
          </div>

          <div class="answer-section">
            <div class="answer-item">
              <span class="label">你的答案：</span>
              <el-tag :type="question.isCorrect ? 'success' : 'danger'">
                {{ formatAnswer(question.studentAnswer, question.type) }}
              </el-tag>
            </div>
            <div class="answer-item" v-if="question.type !== 4">
              <span class="label">正确答案：</span>
              <el-tag type="success">
                {{ formatAnswer(question.correctAnswer, question.type) }}
              </el-tag>
            </div>
          </div>

          <div class="analysis-section" v-if="question.analysis">
            <div class="analysis-title">
              <el-icon><InfoFilled /></el-icon>
              解析：
            </div>
            <div class="analysis-content">{{ question.analysis }}</div>
          </div>
        </el-card>

        <el-empty v-if="questionList.length === 0" description="暂无答题记录" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, InfoFilled } from '@element-plus/icons-vue'
import { getExamRecordDetail } from '@/api/student'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const examDetail = ref({})
const questionList = ref([])

// 返回列表
const goBack = () => {
  router.back()
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

// 获取题目类型文本
const getTypeText = (type) => {
  return { 1: '单选题', 2: '多选题', 3: '判断题', 4: '简答题' }[type] || '未知'
}

// 获取题目类型标签类型
const getTypeTagType = (type) => {
  return { 1: '', 2: 'warning', 3: 'success', 4: 'info' }[type] || 'info'
}

// 格式化答案显示
const formatAnswer = (answer, type) => {
  if (!answer) return '-'
  if (type === 1 || type === 3) {
    return answer
  } else if (type === 2) {
    return answer.split(',').join(', ')
  }
  return answer
}

// 获取分数标签类型
const getScoreType = (score) => {
  if (score >= 90) return 'success'
  if (score >= 80) return ''
  if (score >= 60) return 'warning'
  return 'danger'
}

// 加载考试详情
const loadExamDetail = async () => {
  const recordId = route.query.recordId
  if (!recordId) {
    ElMessage.error('缺少考试记录ID')
    goBack()
    return
  }

  loading.value = true
  try {
    const res = await getExamRecordDetail(recordId)
    if (res.code === 200 && res.data) {
      examDetail.value = res.data.exam || {}
      questionList.value = res.data.questions || []
    }
  } catch (error) {
    console.error('加载考试详情失败:', error)
    ElMessage.error('加载考试详情失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadExamDetail()
})
</script>

<style scoped lang="scss">
.score-detail-page {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  
  h2 {
    margin: 0;
    font-size: 24px;
    color: #303133;
  }
}

.info-card {
  margin-bottom: 20px;
  border-radius: 8px;
  
  .info-item {
    display: flex;
    align-items: center;
    gap: 8px;
    
    .label {
      color: #909399;
      font-size: 14px;
    }
    
    span:not(.label) {
      color: #606266;
      font-size: 14px;
    }
  }
}

.questions-card {
  border-radius: 8px;
  
  .question-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  
  .question-item {
    border-left: 4px solid #409EFF;
    
    .question-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;
      
      .question-index {
        flex: 1;
        font-size: 16px;
        font-weight: bold;
        color: #303133;
      }
    }
    
    .question-content {
      margin-bottom: 16px;
      padding: 12px;
      background: #f5f7fa;
      border-radius: 6px;
      
      .content-label {
        font-weight: bold;
        color: #606266;
        margin-bottom: 8px;
      }
      
      .content-text {
        color: #303133;
        line-height: 1.6;
        white-space: pre-wrap;
      }
    }
    
    .answer-section {
      display: flex;
      gap: 20px;
      margin-bottom: 16px;
      
      .answer-item {
        display: flex;
        align-items: center;
        gap: 8px;
        
        .label {
          font-weight: bold;
          color: #606266;
        }
      }
    }
    
    .analysis-section {
      padding: 12px;
      background: #f0f9ff;
      border-radius: 6px;
      border-left: 3px solid #409EFF;
      
      .analysis-title {
        display: flex;
        align-items: center;
        gap: 6px;
        font-weight: bold;
        color: #409EFF;
        margin-bottom: 8px;
      }
      
      .analysis-content {
        color: #606266;
        line-height: 1.6;
        white-space: pre-wrap;
      }
    }
  }
}

:deep(.el-card__body) {
  padding: 20px;
}
</style>
