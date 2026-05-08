<template>
  <div class="wrong-book-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>错题本</h2>
      <p>查看和复习做错的题目</p>
    </div>

    <!-- 筛选条件 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="考试名称">
          <el-input v-model="filterForm.examName" placeholder="请输入考试名称" clearable />
        </el-form-item>
        <el-form-item label="题目类型">
          <el-select v-model="filterForm.questionType" placeholder="请选择题目类型" clearable>
            <el-option label="单选题" :value="1" />
            <el-option label="多选题" :value="2" />
            <el-option label="判断题" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleFilter">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 错题列表 -->
    <div class="wrong-list" v-loading="loading">
      <el-card v-for="wrong in filteredWrongList" :key="wrong.id" class="wrong-card">
        <div class="wrong-header">
          <el-tag type="danger" size="large">错题</el-tag>
          <el-tag :type="getTypeTagType(wrong.questionType)" size="large">
            {{ getTypeText(wrong.questionType) }}
          </el-tag>
          <span class="exam-name">{{ wrong.examName }}</span>
          <span class="submit-time">{{ formatDateTime(wrong.submitTime) }}</span>
        </div>
        
        <div class="wrong-content">
          <div class="question-title">
            <strong>题目：</strong>{{ wrong.questionContent }}
          </div>
          
          <div class="answer-section">
            <div class="student-answer">
              <span class="label">你的答案：</span>
              <el-tag type="danger">{{ formatAnswer(wrong.studentAnswer, wrong.questionType) }}</el-tag>
            </div>
            <div class="correct-answer">
              <span class="label">正确答案：</span>
              <el-tag type="success">{{ formatAnswer(wrong.correctAnswer, wrong.questionType) }}</el-tag>
            </div>
          </div>
          
          <div class="analysis-section" v-if="wrong.analysis">
            <div class="analysis-title">
              <el-icon><InfoFilled /></el-icon>
              解析：
            </div>
            <div class="analysis-content">{{ wrong.analysis }}</div>
          </div>
        </div>
      </el-card>

      <!-- 空状态 -->
      <el-empty v-if="filteredWrongList.length === 0 && !loading" description="暂无错题记录">
        <el-button type="primary" @click="refreshList">刷新</el-button>
      </el-empty>
    </div>

    <!-- 统计信息 -->
    <el-card class="stats-card" shadow="never" v-if="wrongList.length > 0">
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">错题总数</div>
            <div class="stat-value" style="color: #f56c6c;">{{ wrongList.length }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">单选题</div>
            <div class="stat-value" style="color: #409eff;">{{ countByType(1) }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">多选题</div>
            <div class="stat-value" style="color: #e6a23c;">{{ countByType(2) }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">判断题</div>
            <div class="stat-value" style="color: #67c23a;">{{ countByType(3) }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, InfoFilled } from '@element-plus/icons-vue'
import { getWrongBookList } from '@/api/student'

const loading = ref(false)
const wrongList = ref([])

const filterForm = reactive({
  examName: '',
  questionType: null
})

// 过滤后的错题列表
const filteredWrongList = computed(() => {
  return wrongList.value.filter(wrong => {
    const matchExam = !filterForm.examName || wrong.examName.includes(filterForm.examName)
    const matchType = filterForm.questionType === null || wrong.questionType === filterForm.questionType
    return matchExam && matchType
  })
})

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
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
  return { 1: '单选题', 2: '多选题', 3: '判断题' }[type] || '未知'
}

// 获取题目类型标签类型
const getTypeTagType = (type) => {
  return { 1: '', 2: 'warning', 3: 'success' }[type] || 'info'
}

// 格式化答案显示
const formatAnswer = (answer, type) => {
  if (!answer) return ''
  if (type === 1 || type === 3) {
    // 单选和判断：显示选项字母
    return answer
  } else if (type === 2) {
    // 多选：显示多个选项
    return answer.split(',').join(', ')
  }
  return answer
}

// 统计指定类型的错题数量
const countByType = (type) => {
  return wrongList.value.filter(w => w.questionType === type).length
}

// 加载错题列表
const loadWrongList = async () => {
  loading.value = true
  try {
    const res = await getWrongBookList()
    if (res.code === 200 && res.data) {
      wrongList.value = res.data
    }
  } catch (error) {
    console.error('加载错题列表失败:', error)
    ElMessage.error('加载错题列表失败')
  } finally {
    loading.value = false
  }
}

// 查询过滤
const handleFilter = () => {
  // 通过computed自动过滤，无需额外操作
}

// 重置筛选条件
const handleReset = () => {
  filterForm.examName = ''
  filterForm.questionType = null
}

// 刷新列表
const refreshList = () => {
  loadWrongList()
  ElMessage.success('刷新成功')
}

onMounted(() => {
  loadWrongList()
})
</script>

<style scoped lang="scss">
.wrong-book-page {
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

.filter-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.wrong-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 20px;
}

.wrong-card {
  border-left: 4px solid #f56c6c;
  transition: all 0.3s;
  
  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    transform: translateY(-2px);
  }
  
  .wrong-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
    
    .exam-name {
      flex: 1;
      font-size: 16px;
      font-weight: bold;
      color: #303133;
    }
    
    .submit-time {
      color: #909399;
      font-size: 14px;
    }
  }
  
  .wrong-content {
    .question-title {
      margin-bottom: 16px;
      padding: 12px;
      background: #f5f7fa;
      border-radius: 6px;
      line-height: 1.6;
    }
    
    .answer-section {
      display: flex;
      gap: 20px;
      margin-bottom: 16px;
      
      .student-answer,
      .correct-answer {
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
      background: #fef0f0;
      border-radius: 6px;
      border-left: 3px solid #f56c6c;
      
      .analysis-title {
        display: flex;
        align-items: center;
        gap: 6px;
        font-weight: bold;
        color: #f56c6c;
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

.stats-card {
  border-radius: 8px;
  
  .stat-item {
    text-align: center;
    padding: 16px;
    
    .stat-label {
      color: #909399;
      font-size: 14px;
      margin-bottom: 8px;
    }
    
    .stat-value {
      font-size: 28px;
      font-weight: bold;
    }
  }
}

:deep(.el-card__body) {
  padding: 20px;
}
</style>
