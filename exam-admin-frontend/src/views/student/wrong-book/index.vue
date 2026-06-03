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
          <el-select v-model="filterForm.examName" placeholder="请选择考试名称" clearable style="width: 200px">
            <el-option 
              v-for="exam in examList" 
              :key="exam" 
              :label="exam" 
              :value="exam"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="科目">
          <el-select v-model="filterForm.subject" placeholder="请选择科目" clearable style="width: 150px">
            <el-option 
              v-for="subject in subjectList" 
              :key="subject" 
              :label="subject" 
              :value="subject"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="题目类型">
          <el-select v-model="filterForm.questionType" placeholder="请选择题目类型" clearable style="width: 150px">
            <el-option label="单选题" :value="1" />
            <el-option label="多选题" :value="2" />
            <el-option label="判断题" :value="3" />
            <el-option label="填空题" :value="4" />
            <el-option label="简答题" :value="5" />
            <el-option label="编程题" :value="6" />
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
      <el-card v-for="wrong in paginatedList" :key="wrong.id" class="wrong-card">
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
              <el-tag :type="wrong.questionType === 4 ? 'info' : 'danger'">{{ formatAnswer(wrong.studentAnswer, wrong.questionType) }}</el-tag>
            </div>
            <div class="correct-answer" v-if="wrong.questionType !== 4">
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
        
        <div class="wrong-actions">
          <el-button type="primary" size="small" @click="practiceQuestion(wrong)">
            <el-icon><Refresh /></el-icon>
            重新练习
          </el-button>
        </div>
      </el-card>

      <!-- 空状态 -->
      <el-empty v-if="filteredWrongList.length === 0 && !loading" description="暂无错题记录">
        <el-button type="primary" @click="refreshList">刷新</el-button>
      </el-empty>
      
      <!-- 分页 -->
      <el-pagination
        v-if="filteredWrongList.length > 0"
        class="pagination"
        background
        layout="total, prev, pager, next"
        :total="filteredWrongList.length"
        :page-size="pageSize"
        v-model:current-page="currentPage"
      />
    </div>

    <!-- 统计信息 -->
    <el-card class="stats-card" shadow="never" v-if="wrongList.length > 0">
      <el-row :gutter="20">
        <el-col :span="4">
          <div class="stat-item">
            <div class="stat-label">错题总数</div>
            <div class="stat-value" style="color: #f56c6c;">{{ wrongList.length }}</div>
          </div>
        </el-col>
        <el-col :span="4">
          <div class="stat-item">
            <div class="stat-label">单选题</div>
            <div class="stat-value" style="color: #409eff;">{{ countByType(1) }}</div>
          </div>
        </el-col>
        <el-col :span="4">
          <div class="stat-item">
            <div class="stat-label">多选题</div>
            <div class="stat-value" style="color: #e6a23c;">{{ countByType(2) }}</div>
          </div>
        </el-col>
        <el-col :span="4">
          <div class="stat-item">
            <div class="stat-label">判断题</div>
            <div class="stat-value" style="color: #67c23a;">{{ countByType(3) }}</div>
          </div>
        </el-col>
        <el-col :span="4">
          <div class="stat-item">
            <div class="stat-label">填空题</div>
            <div class="stat-value" style="color: #909399;">{{ countByType(4) }}</div>
          </div>
        </el-col>
        <el-col :span="4">
          <div class="stat-item">
            <div class="stat-label">简答题</div>
            <div class="stat-value" style="color: #f56c6c;">{{ countByType(5) }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>
    
    <!-- 练习对话框 -->
    <el-dialog v-model="practiceDialogVisible" title="错题练习" width="700px" :close-on-click-modal="false">
      <div v-if="currentQuestion" class="practice-content">
        <div class="question-header">
          <el-tag :type="getTypeTagType(currentQuestion.questionType)">{{ getTypeText(currentQuestion.questionType) }}</el-tag>
          <span class="question-source">来自：{{ currentQuestion.examName }}</span>
        </div>
        
        <div class="question-body">
          <p class="question-text">{{ currentQuestion.questionContent }}</p>
          
          <!-- 单选题选项 -->
          <div v-if="currentQuestion.questionType === 1" class="options-list">
            <div 
              v-for="(option, index) in parseOptions(currentQuestion.options)" 
              :key="index"
              class="option-item"
              :class="{ selected: practiceAnswer === String.fromCharCode(65 + index) }"
              @click="practiceAnswer = String.fromCharCode(65 + index)"
            >
              <span class="option-label">{{ String.fromCharCode(65 + index) }}.</span>
              <span class="option-text">{{ option }}</span>
            </div>
          </div>
          
          <!-- 多选题选项 -->
          <div v-if="currentQuestion.questionType === 2" class="options-list">
            <div 
              v-for="(option, index) in parseOptions(currentQuestion.options)" 
              :key="index"
              class="option-item multiple"
              :class="{ selected: practiceMultipleAnswer.includes(String.fromCharCode(65 + index)) }"
              @click="toggleMultipleAnswer(String.fromCharCode(65 + index))"
            >
              <span class="option-label">{{ String.fromCharCode(65 + index) }}.</span>
              <span class="option-text">{{ option }}</span>
            </div>
          </div>
          
          <!-- 判断题 -->
          <div v-if="currentQuestion.questionType === 3" class="true-false-options">
            <el-button 
              :type="practiceAnswer === 'true' ? 'primary' : ''" 
              @click="practiceAnswer = 'true'"
            >正确</el-button>
            <el-button 
              :type="practiceAnswer === 'false' ? 'primary' : ''" 
              @click="practiceAnswer = 'false'"
            >错误</el-button>
          </div>
          
          <!-- 填空题 -->
          <div v-if="currentQuestion.questionType === 4" class="fill-blank-input">
            <el-input 
              v-model="practiceAnswer" 
              type="textarea" 
              :rows="3" 
              placeholder="请输入答案（多个答案用逗号分隔）"
            />
          </div>
          
          <!-- 简答题 -->
          <div v-if="currentQuestion.questionType === 5" class="short-answer-input">
            <el-input 
              v-model="practiceAnswer" 
              type="textarea" 
              :rows="5" 
              placeholder="请输入你的答案"
            />
          </div>
        </div>
        
        <div class="practice-footer">
          <el-button @click="practiceDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="submitPractice" 
            :disabled="currentQuestion?.questionType === 2 ? practiceMultipleAnswer.length === 0 : !practiceAnswer"
          >提交答案</el-button>
        </div>
        
        <!-- 练习结果 -->
        <div v-if="practiceResult" class="practice-result" :class="practiceResult.isCorrect ? 'correct' : 'incorrect'">
          <div class="result-header">
            <el-icon v-if="practiceResult.isCorrect" color="#67c23a"><CircleCheck /></el-icon>
            <el-icon v-else color="#f56c6c"><CircleClose /></el-icon>
            <span>{{ practiceResult.isCorrect ? '回答正确！' : '回答错误' }}</span>
          </div>
          <div class="result-detail" v-if="!practiceResult.isCorrect">
            <p><strong>正确答案：</strong>{{ formatAnswer(currentQuestion.correctAnswer, currentQuestion.questionType) }}</p>
            <p v-if="currentQuestion.analysis"><strong>解析：</strong>{{ currentQuestion.analysis }}</p>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, InfoFilled, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { getWrongBookList } from '@/api/student'
import request from '@/utils/request'

const loading = ref(false)
const wrongList = ref([])
const examList = ref([])
const subjectList = ref([])
const currentPage = ref(1)
const pageSize = 10

// 练习对话框
const practiceDialogVisible = ref(false)
const currentQuestion = ref(null)
const practiceAnswer = ref('')
const practiceMultipleAnswer = ref([])
const practiceResult = ref(null)

const filterForm = reactive({
  examName: '',
  subject: '',
  questionType: null
})

// 过滤后的错题列表
const filteredWrongList = computed(() => {
  return wrongList.value.filter(wrong => {
    const matchExam = !filterForm.examName || wrong.examName === filterForm.examName
    const matchSubject = !filterForm.subject || wrong.subject === filterForm.subject
    // 使用 == null 同时处理 null 和 undefined
    const matchType = filterForm.questionType == null || wrong.questionType === filterForm.questionType
    return matchExam && matchSubject && matchType
  })
})

// 分页后的错题列表
const paginatedList = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  const end = start + pageSize
  return filteredWrongList.value.slice(start, end)
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
  return { 1: '单选题', 2: '多选题', 3: '判断题', 4: '填空题', 5: '简答题', 6: '编程题' }[type] || '未知'
}

// 获取题目类型标签类型
const getTypeTagType = (type) => {
  return { 1: '', 2: 'warning', 3: 'success', 4: 'info', 5: 'danger', 6: 'primary' }[type] || 'info'
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

// 加载考试和科目列表
const loadExamAndSubjects = async () => {
  try {
    // 从错题列表中提取所有考试名称和科目
    const exams = [...new Set(wrongList.value.map(w => w.examName).filter(Boolean))]
    const subjects = [...new Set(wrongList.value.map(w => w.subject).filter(Boolean))]
    examList.value = exams
    subjectList.value = subjects
  } catch (error) {
    console.error('加载考试和科目列表失败:', error)
  }
}

// 加载错题列表
const loadWrongList = async () => {
  loading.value = true
  try {
    const res = await getWrongBookList()
    if (res.code === 200 && res.data) {
      wrongList.value = res.data
      // 加载考试和科目列表
      loadExamAndSubjects()
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
  filterForm.subject = ''
  filterForm.questionType = null
}

// 刷新列表
const refreshList = () => {
  loadWrongList()
  ElMessage.success('刷新成功')
}

// 解析选项
const parseOptions = (optionsData) => {
  console.log('【调试】解析选项，原始数据:', optionsData)
  
  if (!optionsData) {
    console.warn('选项数据为空')
    return []
  }
  
  // 如果已经是数组（后端可能自动解析了）
  if (Array.isArray(optionsData)) {
    return optionsData
  }
  
  try {
    // 尝试解析JSON字符串
    const parsed = JSON.parse(optionsData)
    if (Array.isArray(parsed)) {
      return parsed
    }
    // 如果是对象格式 {"A": "选项1", "B": "选项2"}，转换为数组
    if (typeof parsed === 'object') {
      return Object.values(parsed)
    }
    return []
  } catch (e) {
    console.error('JSON解析失败:', e)
    // 如果不是JSON，尝试用|分隔
    if (typeof optionsData === 'string') {
      return optionsData.split('|').filter(o => o.trim())
    }
    return []
  }
}

// 打开练习对话框
const practiceQuestion = (question) => {
  currentQuestion.value = question
  practiceAnswer.value = ''
  practiceMultipleAnswer.value = []
  practiceResult.value = null
  practiceDialogVisible.value = true
}

// 切换多选题选项
const toggleMultipleAnswer = (option) => {
  const index = practiceMultipleAnswer.value.indexOf(option)
  if (index > -1) {
    practiceMultipleAnswer.value.splice(index, 1)
  } else {
    practiceMultipleAnswer.value.push(option)
  }
}

// 提交练习答案
const submitPractice = () => {
  if (!currentQuestion.value) return
  
  const question = currentQuestion.value
  
  // 验证是否已选择答案
  if (question.questionType === 2) {
    // 多选题
    if (practiceMultipleAnswer.value.length === 0) {
      ElMessage.warning('请至少选择一个选项')
      return
    }
  } else {
    // 其他题型
    if (!practiceAnswer.value) {
      ElMessage.warning('请输入答案')
      return
    }
  }
  
  let isCorrect = false
  
  if (question.questionType === 1) {
    // 单选题
    isCorrect = practiceAnswer.value === question.correctAnswer
  } else if (question.questionType === 2) {
    // 多选题
    const studentAns = practiceMultipleAnswer.value.sort().join('')
    const correctAns = question.correctAnswer.split(',').sort().join('')
    isCorrect = studentAns === correctAns
  } else if (question.questionType === 3) {
    // 判断题：标准化后比较
    const normalizeAnswer = (ans) => {
      const a = ans.toLowerCase().trim()
      if (a === '正确' || a === '√' || a === 't' || a === 'true' || a === '1' || a === '对') return 'T'
      if (a === '错误' || a === '×' || a === 'x' || a === 'f' || a === 'false' || a === '0' || a === '错') return 'F'
      return a
    }
    isCorrect = normalizeAnswer(practiceAnswer.value) === normalizeAnswer(question.correctAnswer)
  } else if (question.questionType === 4) {
    // 填空题
    const studentAns = practiceAnswer.value.trim()
    const correctAns = question.correctAnswer.trim()
    isCorrect = studentAns === correctAns
  } else if (question.questionType === 5) {
    // 简答题（简单判断是否填写）
    isCorrect = practiceAnswer.value.trim().length > 0
  }
  
  practiceResult.value = { isCorrect }
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
  
  .pagination {
    display: flex;
    justify-content: center;
    margin-top: 20px;
  }
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
  
  .wrong-actions {
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px dashed #e4e7ed;
    display: flex;
    justify-content: flex-end;
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

.practice-content {
  .question-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 20px;
    padding-bottom: 16px;
    border-bottom: 1px solid #e4e7ed;
    
    .question-source {
      color: #909399;
      font-size: 14px;
    }
  }
  
  .question-body {
    .question-text {
      font-size: 16px;
      line-height: 1.8;
      color: #303133;
      margin-bottom: 20px;
    }
    
    .options-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
      
      .option-item {
        padding: 12px 16px;
        border: 2px solid #e4e7ed;
        border-radius: 8px;
        cursor: pointer;
        transition: all 0.3s;
        display: flex;
        align-items: center;
        gap: 12px;
        
        &:hover {
          border-color: #409eff;
          background: #ecf5ff;
        }
        
        &.selected {
          border-color: #409eff;
          background: #ecf5ff;
        }
        
        &.multiple.selected {
          border-color: #67c23a;
          background: #f0f9eb;
        }
        
        .option-label {
          font-weight: bold;
          color: #409eff;
          min-width: 24px;
        }
        
        .option-text {
          flex: 1;
          color: #606266;
        }
      }
    }
    
    .true-false-options {
      display: flex;
      gap: 16px;
      
      .el-button {
        flex: 1;
        height: 48px;
        font-size: 16px;
      }
    }
    
    .fill-blank-input,
    .short-answer-input {
      margin-top: 16px;
    }
  }
  
  .practice-footer {
    margin-top: 24px;
    padding-top: 20px;
    border-top: 1px solid #e4e7ed;
    display: flex;
    justify-content: flex-end;
    gap: 12px;
  }
  
  .practice-result {
    margin-top: 20px;
    padding: 16px;
    border-radius: 8px;
    
    &.correct {
      background: #f0f9eb;
      border: 1px solid #e1f3d8;
      
      .result-header {
        color: #67c23a;
      }
    }
    
    &.incorrect {
      background: #fef0f0;
      border: 1px solid #fde2e2;
      
      .result-header {
        color: #f56c6c;
      }
    }
    
    .result-header {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 18px;
      font-weight: bold;
      margin-bottom: 12px;
    }
    
    .result-detail {
      p {
        margin: 8px 0;
        line-height: 1.6;
        color: #606266;
        
        strong {
          color: #303133;
        }
      }
    }
  }
}
</style>
