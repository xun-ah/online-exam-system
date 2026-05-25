<template>
  <div class="online-exam-container" v-loading="loading">
    <!-- 考试头部信息 -->
    <div class="exam-header">
      <div class="exam-info">
        <h2>{{ examData.examName }}</h2>
        <div class="exam-meta">
          <span>试卷：{{ examData.paperName }}</span>
          <span>总分：{{ examData.totalScore }}分</span>
          <span>时长：{{ examData.duration }}分钟</span>
        </div>
      </div>
      <div class="timer-box" :class="{ 'timer-warning': remainingTime < 300 }">
        <div class="timer-icon">⏱️</div>
        <div class="timer-text">{{ formatTime(remainingTime) }}</div>
      </div>
    </div>

    <!-- 题目列表 -->
    <div class="questions-list" v-if="questions.length > 0">
      <div 
        v-for="(question, index) in questions" 
        :key="question.questionId"
        class="question-card"
        :id="'question-' + question.questionId"
      >
        <div class="question-header">
          <span class="question-number">{{ question.questionNumber }}.</span>
          <el-tag :type="getTypeTagType(question.type)" size="small">{{ getTypeText(question.type) }}</el-tag>
          <span class="question-score">（{{ question.score }}分）</span>
        </div>
        
        <!-- 题目内容（填空题除外，填空题在自己的容器中显示） -->
        <div v-if="question.type !== 4" class="question-content">{{ question.content }}</div>
        
        <!-- 单选题/多选题选项 -->
        <div v-if="question.type === 1 || question.type === 2" class="options-list">
          <div 
            v-for="option in ['A', 'B', 'C', 'D']" 
            :key="option"
            class="option-item"
            :class="{ 'selected': isOptionSelected(question, option) }"
            @click="selectOption(question, option)"
          >
            <span class="option-label">{{ option }}.</span>
            <span class="option-text">{{ question['option' + option] || '（该选项暂无内容）' }}</span>
          </div>
        </div>
        
        <!-- 判断题 -->
        <div v-if="question.type === 3" class="options-list">
          <div 
            class="option-item"
            :class="{ 'selected': answers[question.questionId] === '正确' }"
            @click="selectJudge(question, '正确')"
          >
            <span class="option-label">✓</span>
            <span class="option-text">正确</span>
          </div>
          <div 
            class="option-item"
            :class="{ 'selected': answers[question.questionId] === '错误' }"
            @click="selectJudge(question, '错误')"
          >
            <span class="option-label">✗</span>
            <span class="option-text">错误</span>
          </div>
        </div>
        
        <!-- 填空题 -->
        <div v-if="question.type === 4" class="fill-blank-container">
          <!-- 显示题目内容（将下划线替换为输入框占位符） -->
          <div class="fill-question-content" v-html="renderFillBlankContent(question)"></div>
          
          <!-- 多个输入框 -->
          <div class="fill-inputs" v-if="getBlankCount(question) > 0">
            <div 
              v-for="(blank, index) in getBlankCount(question)" 
              :key="index"
              class="fill-input-item"
            >
              <span class="blank-label">第{{ index + 1 }}空：</span>
              <el-input 
                v-model="fillBlankAnswers[question.questionId][index]"
                placeholder="请输入答案"
                @input="updateFillBlankAnswer(question.questionId)"
              />
            </div>
          </div>
          
          <!-- 如果没有检测到空位，显示单个输入框 -->
          <el-input 
            v-if="getBlankCount(question) === 0"
            v-model="answers[question.questionId]"
            type="textarea"
            :rows="2"
            placeholder="请输入答案"
          />
        </div>
        
        <!-- 简答题 -->
        <div v-if="question.type === 5" class="essay-input">
          <el-input 
            v-model="answers[question.questionId]"
            type="textarea"
            :rows="6"
            placeholder="请输入答案"
          />
        </div>
        
        <!-- 试卷结束标记 -->
        <div v-if="index === questions.length - 1" class="exam-end-marker">
          — 试卷结束 —
        </div>
      </div>
    </div>



    <!-- 答题卡（右侧悬浮） -->
    <div class="answer-sheet">
      <!-- 操作按钮在顶部 -->
      <div class="sheet-actions">
        <el-button @click="handleSave" :disabled="!started" :loading="isAutoSaving" size="default" style="width: 48%; padding: 12px 20px; font-size: 14px;">
          保存
        </el-button>
        <el-button type="primary" @click="handleSubmit" :disabled="!started" size="default" style="width: 48%; padding: 12px 20px; font-size: 14px;">提交</el-button>
      </div>
      
      <!-- 自动保存状态 -->
      <div v-if="lastAutoSaveTime" class="auto-save-status">
        <span class="status-dot" :class="{ 'saving': isAutoSaving }"></span>
        <span class="status-text">
          {{ isAutoSaving ? '保存中...' : `已自动保存 ${lastAutoSaveTime}` }}
        </span>
      </div>
      
      <!-- 防切屏警告 -->
      <div v-if="hasSwitchWarning" class="switch-warning">
        <span class="warning-icon">️</span>
        <span class="warning-text">切屏警告：{{ switchCount }}/{{ maxSwitchCount }}</span>
      </div>
      
      <h4>答题卡</h4>
      <div class="sheet-grid">
        <div 
          v-for="question in questions" 
          :key="question.questionId"
          class="sheet-item"
          :class="{ 'answered': answers[question.questionId] }"
          @click="scrollToQuestion(question.questionId)"
        >
          {{ question.questionNumber }}
        </div>
      </div>
      <div class="sheet-legend">
        <span class="legend-item"><span class="dot answered"></span>已答</span>
        <span class="legend-item"><span class="dot"></span>未答</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getExamDetail, startExam, submitAnswer, submitExam } from '@/api/student'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const started = ref(false)
const recordId = ref(null)
const examData = reactive({
  examId: null,
  examName: '',
  paperName: '',
  duration: 120,
  totalScore: 0,
  endTime: '' // 考试结束时间
})

const questions = ref([])
const answers = reactive({})

// 填空题多答案存储：{ questionId: [answer1, answer2, ...] }
const fillBlankAnswers = reactive({})

// 倒计时
const remainingTime = ref(0)
let timer = null
let autoSaveTimer = null
const isAutoSaving = ref(false)
const lastAutoSaveTime = ref(null)

// 防切屏监控
const switchCount = ref(0)
const maxSwitchCount = 3 // 最大允许切屏次数
const hasSwitchWarning = ref(false)

// 开始倒计时
const startTimer = () => {
  // 计算剩余时间：考试结束时间 - 当前电脑时间
  const now = new Date().getTime()
  const end = new Date(examData.endTime).getTime()
  const remainingSeconds = Math.floor((end - now) / 1000)
  remainingTime.value = Math.max(0, remainingSeconds)
  
  timer = setInterval(() => {
    if (remainingTime.value > 0) {
      remainingTime.value--
    } else {
      clearInterval(timer)
      ElMessage.warning('考试时间到，自动交卷')
      handleSubmit(true) // 强制交卷
    }
  }, 1000)
}

// 格式化时间
const formatTime = (seconds) => {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

// 获取题型文本
const getTypeText = (type) => {
  const typeMap = { 1: '单选题', 2: '多选题', 3: '判断题', 4: '填空题', 5: '简答题' }
  return typeMap[type] || ''
}

// 获取题型标签类型
const getTypeTagType = (type) => {
  const typeMap = { 1: 'success', 2: 'warning', 3: 'info', 5: 'danger' }
  return typeMap[type] || null
}

// 判断选项是否被选中
const isOptionSelected = (question, option) => {
  const answer = answers[question.questionId]
  if (!answer) return false
  if (question.type === 1) {
    return answer === option
  } else if (question.type === 2) {
    return answer.includes(option)
  }
  return false
}

// 选择选项（单选）
const selectOption = (question, option) => {
  if (question.type === 1) {
    answers[question.questionId] = option
  } else if (question.type === 2) {
    // 多选
    if (!answers[question.questionId]) {
      answers[question.questionId] = ''
    }
    if (answers[question.questionId].includes(option)) {
      answers[question.questionId] = answers[question.questionId].replace(option, '')
    } else {
      answers[question.questionId] += option
    }
  }
}

// 选择判断
const selectJudge = (question, value) => {
  answers[question.questionId] = value
}

// 滚动到指定题目
const scrollToQuestion = (questionId) => {
  const element = document.getElementById('question-' + questionId)
  if (element) {
    element.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}

// 自动保存答案（静默保存）
const autoSaveAnswers = async () => {
  if (!started.value || Object.keys(answers).length === 0) return
  
  try {
    isAutoSaving.value = true
    sessionStorage.setItem('exam_answers_' + examData.examId, JSON.stringify(answers))
    
    const answerList = Object.keys(answers).map(questionId => ({
      questionId: parseInt(questionId),
      answer: answers[questionId]
    }))
    
    await submitAnswer({
      recordId: recordId.value,
      answers: JSON.stringify(answerList)
    })
    
    lastAutoSaveTime.value = new Date().toLocaleTimeString()
  } catch (error) {
    console.error('自动保存失败:', error)
  } finally {
    isAutoSaving.value = false
  }
}

// 初始化防切屏监控
const initSwitchMonitor = () => {
  // 只监听页面可见性变化，避免重复计数
  document.addEventListener('visibilitychange', handleVisibilityChange)
}

// 移除防切屏监控
const removeSwitchMonitor = () => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
}

// 处理页面可见性变化
const handleVisibilityChange = () => {
  if (document.hidden && started.value) {
    handleSwitchScreen()
  }
}

// 处理窗口失去焦点（已禁用，避免重复计数）
// const handleWindowBlur = () => {
//   if (started.value) {
//     handleSwitchScreen()
//   }
// }

// 处理切屏行为
const handleSwitchScreen = () => {
  switchCount.value++
  
  if (switchCount.value >= maxSwitchCount) {
    // 超过限制，自动交卷（强制交卷，跳过确认）
    ElMessageBox.alert(
      `检测到您已切屏 ${switchCount.value} 次，超过最大允许次数（${maxSwitchCount}次），系统将自动交卷。`,
      '违规警告',
      {
        type: 'error',
        confirmButtonText: '确定',
        showClose: false,
        closeOnClickModal: false,
        closeOnPressEscape: false
      }
    ).then(() => {
      handleSubmit(true) // 强制交卷
    })
  } else {
    // 警告提示
    const remaining = maxSwitchCount - switchCount.value
    ElMessage.warning(`️ 检测到切屏行为！剩余警告次数：${remaining} 次，超过 ${maxSwitchCount} 次将自动交卷`)
    hasSwitchWarning.value = true
  }
}

// 计算填空题的空位数量（通过下划线数量判断）
const getBlankCount = (question) => {
  if (!question.content) return 0
  // 匹配连续的下划线（至少2个）
  const matches = question.content.match(/_{2,}/g)
  return matches ? matches.length : 0
}

// 渲染填空题内容（将下划线替换为占位符）
const renderFillBlankContent = (question) => {
  if (!question.content) return ''
  // 将连续下划线替换为带样式的占位符
  return question.content.replace(/_{2,}/g, '<span class="blank-placeholder">______</span>')
}

// 更新填空题答案（将多个空的答案合并为一个字符串，用 | 分隔）
const updateFillBlankAnswer = (questionId) => {
  const answers_array = fillBlankAnswers[questionId] || []
  // 过滤空值并合并
  const validAnswers = answers_array.filter(a => a && a.trim())
  answers[questionId] = validAnswers.join('|')
}

// 初始化填空题答案数组
const initFillBlankAnswers = () => {
  questions.value.forEach(question => {
    if (question.type === 4) {
      const blankCount = getBlankCount(question)
      if (blankCount > 0 && !fillBlankAnswers[question.questionId]) {
        fillBlankAnswers[question.questionId] = new Array(blankCount).fill('')
      }
    }
  })
}

// 保存答案
const handleSave = async () => {
  try {
    // 保存到sessionStorage，防止页面刷新丢失
    sessionStorage.setItem('exam_answers_' + examData.examId, JSON.stringify(answers))
    
    // 将答案转换为JSON字符串保存到后端
    const answerList = Object.keys(answers).map(questionId => ({
      questionId: parseInt(questionId),
      answer: answers[questionId]
    }))
    
    // 调用后端保存接口
    await submitAnswer({
      recordId: recordId.value,
      answers: JSON.stringify(answerList)
    })
    
    lastAutoSaveTime.value = new Date().toLocaleTimeString()
    ElMessage.success('答案已保存')
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  }
}

// 提交试卷（force参数：true=强制交卷，跳过确认）
const handleSubmit = async (force = false) => {
  // 先停止自动保存
  if (autoSaveTimer) {
    clearInterval(autoSaveTimer)
  }
  
  try {
    // 强制交卷时跳过确认框
    if (!force) {
      await ElMessageBox.confirm('确认提交试卷？提交后将无法修改。', '提示', {
        type: 'warning'
      })
    }
    
    const answerList = Object.keys(answers).map(questionId => ({
      questionId: parseInt(questionId),
      answer: answers[questionId]
    }))
    
    await submitExam(examData.examId, {
      recordId: recordId.value,
      answers: answerList
    })
    
    ElMessage.success('提交成功')
    clearInterval(timer)
    router.push('/student/exam-record')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('提交失败')
    }
  }
}

// 初始化考试
const initExam = async () => {
  loading.value = true
  try {
    const examId = route.query.examId
    if (!examId) {
      ElMessage.error('考试ID不存在')
      router.back()
      return
    }
    
    // 从sessionStorage恢复答案
    const savedAnswers = sessionStorage.getItem('exam_answers_' + examId)
    if (savedAnswers) {
      try {
        Object.assign(answers, JSON.parse(savedAnswers))
      } catch (e) {
        console.error('恢复答案失败:', e)
      }
    }
    
    // 获取考试详情
    const res = await getExamDetail(examId)
    if (res.code === 200 && res.data) {
      Object.assign(examData, res.data)
      questions.value = res.data.questions || []
      
      // 初始化填空题答案数组
      initFillBlankAnswers()
      
      // 调试：打印第一题的数据结构，检查选项是否返回
      if (questions.value.length > 0) {
        console.log('题目数据示例:', questions.value[0])
        console.log('题目选项:', {
          A: questions.value[0].optionA,
          B: questions.value[0].optionB,
          C: questions.value[0].optionC,
          D: questions.value[0].optionD
        })
      }
      
      // 开始考试
      const startRes = await startExam(examId)
      if (startRes.code === 200) {
        recordId.value = startRes.data.recordId
        started.value = true
        // 使用考试结束时间计算剩余时间
        startTimer()
        
        // 启动自动保存：每60秒自动保存一次
        autoSaveTimer = setInterval(() => {
          autoSaveAnswers()
        }, 60000)
        
        // 启动防切屏监控
        initSwitchMonitor()
      }
    }
  } catch (error) {
    console.error('初始化考试失败:', error)
    ElMessage.error('加载考试失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  initExam()
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
  if (autoSaveTimer) {
    clearInterval(autoSaveTimer)
  }
  // 移除防切屏监控
  removeSwitchMonitor()
})
</script>

<style scoped lang="scss">
.online-exam-container {
  padding: 20px;
  padding-right: 200px;
  position: relative;
  min-height: calc(100vh - 56px);
  background: #f5f7fa;
}

.exam-header {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  
  .exam-info {
    h2 {
      margin: 0 0 10px 0;
      font-size: 24px;
      color: #303133;
    }
    
    .exam-meta {
      span {
        margin-right: 20px;
        color: #909399;
        font-size: 14px;
      }
    }
  }
  
  .timer-box {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    padding: 15px 25px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    gap: 10px;
    
    &.timer-warning {
      background: linear-gradient(135deg, #f56565 0%, #e53e3e 100%);
      animation: pulse 1s infinite;
    }
    
    .timer-icon {
      font-size: 24px;
    }
    
    .timer-text {
      font-size: 24px;
      font-weight: bold;
      font-family: monospace;
    }
  }
}

.questions-list {
  .question-card {
    background: #fff;
    padding: 25px;
    border-radius: 8px;
    margin-bottom: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    
    .question-header {
      margin-bottom: 15px;
      display: flex;
      align-items: center;
      gap: 10px;
      
      .question-number {
        font-size: 18px;
        font-weight: bold;
        color: #409eff;
      }
      
      .question-score {
        color: #909399;
        font-size: 14px;
      }
    }
    
    .question-content {
      font-size: 16px;
      line-height: 1.8;
      color: #303133;
      margin-bottom: 20px;
    }
    
    .options-list {
      .option-item {
        padding: 12px 15px;
        margin-bottom: 10px;
        border: 2px solid #e4e7ed;
        border-radius: 6px;
        cursor: pointer;
        transition: all 0.3s;
        display: flex;
        align-items: center;
        gap: 10px;
        
        &:hover {
          border-color: #409eff;
          background: #ecf5ff;
        }
        
        &.selected {
          border-color: #409eff;
          background: #ecf5ff;
        }
        
        .option-label {
          font-weight: bold;
          color: #409eff;
          min-width: 30px;
        }
        
        .option-text {
          flex: 1;
        }
      }
    }
    
    .fill-input,
    .essay-input {
      margin-top: 15px;
    }
    
    .fill-blank-container {
      .fill-question-content {
        font-size: 16px;
        line-height: 1.8;
        color: #303133;
        margin-bottom: 20px;
        
        .blank-placeholder {
          display: inline-block;
          min-width: 100px;
          border-bottom: 2px solid #409eff;
          color: #409eff;
          font-weight: bold;
          text-align: center;
          padding: 0 5px;
        }
      }
      
      .fill-inputs {
        display: flex;
        flex-direction: column;
        gap: 12px;
        margin-top: 15px;
        
        .fill-input-item {
          display: flex;
          align-items: center;
          gap: 10px;
          
          .blank-label {
            font-size: 14px;
            color: #606266;
            font-weight: 500;
            white-space: nowrap;
          }
          
          :deep(.el-input) {
            flex: 1;
          }
        }
      }
    }
    
    .exam-end-marker {
      text-align: center;
      padding: 20px;
      color: #909399;
      font-size: 16px;
      font-weight: bold;
      margin-top: 20px;
      border-top: 2px dashed #e4e7ed;
    }
  }
}

.answer-sheet {
  position: fixed;
  right: 20px;
  top: 80px;
  width: 240px;
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  
  .sheet-actions {
    display: flex;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 15px;
    padding-bottom: 15px;
    border-bottom: 1px solid #e4e7ed;
    
    button {
      flex: 1;
    }
  }
  
  .auto-save-status {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px;
    margin-bottom: 15px;
    background: #f0f9ff;
    border-radius: 6px;
    font-size: 12px;
    color: #409eff;
    
    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #67c23a;
      flex-shrink: 0;
      
      &.saving {
        background: #e6a23c;
        animation: blink 1s infinite;
      }
    }
    
    .status-text {
      flex: 1;
    }
  }
  
  .switch-warning {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px;
    margin-bottom: 15px;
    background: #fef0f0;
    border: 1px solid #fbc4c4;
    border-radius: 6px;
    font-size: 12px;
    color: #f56c6c;
    animation: shake 0.5s;
    
    .warning-icon {
      font-size: 16px;
    }
    
    .warning-text {
      font-weight: bold;
    }
  }
  
  h4 {
    margin: 0 0 15px 0;
    text-align: center;
    color: #303133;
  }
  
  .sheet-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 8px;
    margin-bottom: 15px;
    
    .sheet-item {
      width: 45px;
      height: 45px;
      display: flex;
      align-items: center;
      justify-content: center;
      border: 1px solid #e4e7ed;
      border-radius: 6px;
      cursor: pointer;
      font-size: 14px;
      transition: all 0.3s;
      
      &:hover {
        border-color: #409eff;
      }
      
      &.answered {
        background: #409eff;
        color: #fff;
        border-color: #409eff;
      }
    }
  }
  
  .sheet-legend {
    display: flex;
    justify-content: space-around;
    font-size: 12px;
    color: #909399;
    
    .legend-item {
      display: flex;
      align-items: center;
      gap: 5px;
      
      .dot {
        width: 10px;
        height: 10px;
        border-radius: 50%;
        background: #e4e7ed;
        
        &.answered {
          background: #409eff;
        }
      }
    }
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.8;
  }
}

@keyframes blink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.3;
  }
}

@keyframes shake {
  0%, 100% {
    transform: translateX(0);
  }
  25% {
    transform: translateX(-5px);
  }
  75% {
    transform: translateX(5px);
  }
}
</style>
