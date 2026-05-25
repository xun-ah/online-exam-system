<template>
  <div class="student-dashboard">
    <!-- 欢迎信息 -->
    <div class="welcome-card">
      <div class="welcome-header">
        <h2>欢迎回来，{{ studentInfo.realName || '同学' }}！</h2>
      </div>
      <div class="welcome-info">
        <span>{{ studentInfo.className || '班级' }}</span>
        <span class="divider">|</span>
        <span>本学期已完成 {{ stats.completedCount }} 场考试</span>
        <span class="divider">|</span>
        <span>平均成绩 {{ stats.avgScore }} 分</span>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card blue">
        <div class="stat-value">{{ stats.pendingCount }}</div>
        <div class="stat-label">待考考试</div>
      </div>
      <div class="stat-card green">
        <div class="stat-value">{{ stats.completedCount }}</div>
        <div class="stat-label">已完成</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-value">{{ stats.avgScore }}</div>
        <div class="stat-label">平均分</div>
      </div>
      <div class="stat-card red">
        <div class="stat-value">{{ stats.classRank }}</div>
        <div class="stat-label">班级排名</div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 左侧：待考考试和最近考试 -->
      <div class="left-section">
        <div class="section-card">
          <h3 class="section-title">待考考试</h3>
          <div v-if="pendingExams.length === 0" class="empty-state">
            <el-empty description="暂无待考考试" />
          </div>
          <div v-else class="exam-list">
            <div 
              v-for="exam in pendingExams" 
              :key="exam.id" 
              class="exam-item"
              :class="{ 'exam-active': canEnterExam(exam) }"
            >
              <div class="exam-info">
                <h4>{{ exam.examName }}</h4>
                <div class="exam-details">
                  <span>考试时间：{{ formatDateTime(exam.startTime) }}</span>
                  <span class="divider">|</span>
                  <span>时长：{{ exam.duration || 0 }}分钟</span>
                  <span class="divider">|</span>
                  <span>总分：{{ exam.totalScore || 0 }}分</span>
                </div>
              </div>
              <div class="exam-action">
                <el-button 
                  v-if="getExamStatus(exam) === '进行中'" 
                  type="primary" 
                  @click="enterExam(exam)"
                >
                  进入考场
                </el-button>
                <el-tag v-else :type="getExamStatusType(exam)" size="small">
                  {{ getExamStatus(exam) }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>

        <!-- 最近考试 -->
        <div class="section-card">
          <h3 class="section-title">最近考试</h3>
          <div v-if="recentExams.length === 0" class="empty-state">
            <el-empty description="暂无考试记录" />
          </div>
          <div v-else class="recent-exam-list">
            <div v-for="record in recentExams" :key="record.id" class="recent-exam-item">
              <span class="exam-name">{{ record.examName }}</span>
              <span class="exam-score" :class="getScoreClass(record.score)">
                {{ record.score }}分
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：成绩趋势 -->
      <div class="right-section">
        <!-- 成绩趋势图 -->
        <div class="section-card">
          <h3 class="section-title">成绩趋势</h3>
          <div class="chart-container">
            <v-chart v-if="scoreTrend.length > 0" class="chart" :option="chartOption" autoresize />
            <el-empty v-else description="暂无考试数据" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, GridComponent, MarkLineComponent } from 'echarts/components'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

// 注册 echarts 组件
use([CanvasRenderer, LineChart, TitleComponent, TooltipComponent, GridComponent, MarkLineComponent])

const router = useRouter()
const userStore = useUserStore()

// 学生信息
const studentInfo = reactive({
  realName: '',
  className: ''
})

// 统计数据
const stats = reactive({
  pendingCount: 0,
  completedCount: 0,
  avgScore: 0,
  classRank: 0
})

// 待考考试
const pendingExams = ref([])

// 最近考试
const recentExams = ref([])

// 成绩趋势数据
const scoreTrend = ref([])

// 图表配置
const chartOption = computed(() => {
  const examNames = scoreTrend.value.map(item => item.examName)
  const scores = scoreTrend.value.map(item => item.score)
  
  // 根据分数设置点的颜色
  const itemColors = scores.map(score => {
    if (score >= 90) return '#67c23a'
    if (score >= 80) return '#409eff'
    if (score >= 60) return '#e6a23c'
    return '#f56c6c'
  })
  
  return {
    tooltip: {
      trigger: 'axis',
      formatter: function(params) {
        const item = params[0]
        const score = item.value
        let status = score >= 60 ? '及格' : '不及格'
        return `${item.name}<br/>分数: ${score}分<br/>状态: ${status}`
      }
    },
    grid: {
      left: '8%',
      right: '4%',
      bottom: '15%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: examNames,
      axisLabel: {
        rotate: 20,
        interval: 0,
        fontSize: 12
      },
      axisLine: {
        lineStyle: { color: '#ddd' }
      }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      axisLine: { lineStyle: { color: '#ddd' } },
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
    },
    series: [
      {
        name: '成绩',
        type: 'line',
        data: scores.map((score, index) => ({
          value: score,
          itemStyle: { color: itemColors[index] }
        })),
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { color: '#409eff', width: 3 },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(64, 158, 255, 0.25)' },
              { offset: 1, color: 'rgba(64, 158, 255, 0.02)' }
            ]
          }
        }
      }
    ],
    // 及格线
    markLine: {
      data: [{
        yAxis: 60,
        name: '及格线',
        lineStyle: { color: '#f56c6c', type: 'dashed', width: 2 },
        label: {
          formatter: '及格线 60分',
          position: 'end',
          color: '#f56c6c',
          fontSize: 11
        }
      }],
      silent: true
    }
  }
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

// 获取考试状态
const getExamStatus = (exam) => {
  const now = new Date().getTime()
  const startTime = new Date(exam.startTime).getTime()
  const endTime = startTime + exam.duration * 60000
  
  if (now < startTime) {
    return '未开始'
  } else if (now >= startTime && now <= endTime) {
    return '进行中'
  } else {
    return '已结束'
  }
}

// 获取考试状态类型（用于Tag颜色）
const getExamStatusType = (exam) => {
  const status = getExamStatus(exam)
  if (status === '未开始') return 'info'
  if (status === '进行中') return 'success'
  return 'danger'
}

// 判断是否可以进入考试
const canEnterExam = (exam) => {
  return getExamStatus(exam) === '进行中'
}

// 进入考试
const enterExam = (exam) => {
  router.push({
    name: 'OnlineExam',
    query: { examId: exam.id }
  })
}

// 获取分数样式类
const getScoreClass = (score) => {
  if (score >= 90) return 'score-excellent'
  if (score >= 80) return 'score-good'
  if (score >= 60) return 'score-pass'
  return 'score-fail'
}

// 加载学生信息
const loadStudentInfo = async () => {
  try {
    const res = await request.get('/student/info')
    if (res.data) {
      studentInfo.realName = res.data.realName || userStore.userInfo?.realName
      studentInfo.className = res.data.className || ''
    }
  } catch (error) {
    console.error('加载学生信息失败:', error)
    studentInfo.realName = userStore.userInfo?.realName || '同学'
  }
}

// 加载统计数据
const loadStats = async () => {
  try {
    const res = await request.get('/student/dashboard/stats')
    if (res.data) {
      stats.pendingCount = res.data.pendingCount || 0
      stats.completedCount = res.data.completedCount || 0
      stats.avgScore = res.data.avgScore || 0
      stats.classRank = res.data.classRank || 0
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 加载待考考试
const loadPendingExams = async () => {
  try {
    const res = await request.get('/student/exams/pending')
    pendingExams.value = res.data || []
  } catch (error) {
    console.error('加载待考考试失败:', error)
  }
}

// 加载最近考试
const loadRecentExams = async () => {
  try {
    const res = await request.get('/student/exams/recent')
    recentExams.value = res.data || []
  } catch (error) {
    console.error('加载最近考试失败:', error)
  }
}

// 加载成绩趋势
const loadScoreTrend = async () => {
  try {
    const res = await request.get('/student/score/trend')
    // 后端已按时间正序返回，直接使用
    scoreTrend.value = res.data || []
  } catch (error) {
    console.error('加载成绩趋势失败:', error)
  }
}

onMounted(() => {
  loadStudentInfo()
  loadStats()
  loadPendingExams()
  loadRecentExams()
  loadScoreTrend()
})
</script>

<style scoped lang="scss">
.student-dashboard {
  padding: 20px;
  background: #f0f2f5;
  min-height: calc(100vh - 56px);
}

.welcome-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

  .welcome-header {
    h2 {
      margin: 0 0 12px 0;
      font-size: 20px;
      color: #303133;
    }
  }

  .welcome-info {
    color: #909399;
    font-size: 14px;

    .divider {
      margin: 0 8px;
    }
  }
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;

  .stat-card {
    background: #fff;
    border-radius: 8px;
    padding: 24px;
    text-align: center;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
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

    &.blue::before {
      background: #409EFF;
    }

    &.green::before {
      background: #67C23A;
    }

    &.orange::before {
      background: #E6A23C;
    }

    &.red::before {
      background: #F56C6C;
    }

    .stat-value {
      font-size: 32px;
      font-weight: bold;
      margin-bottom: 8px;

      .blue & {
        color: #409EFF;
      }

      .green & {
        color: #67C23A;
      }

      .orange & {
        color: #E6A23C;
      }

      .red & {
        color: #F56C6C;
      }
    }

    .stat-label {
      color: #909399;
      font-size: 14px;
    }
  }
}

.main-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;

  .left-section,
  .right-section {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
}

.section-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

  .section-title {
    margin: 0 0 16px 0;
    font-size: 16px;
    color: #303133;
    font-weight: 600;
  }
}

.exam-list {
  .exam-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px;
    margin-bottom: 12px;
    background: #fafafa;
    border-radius: 8px;
    border-left: 4px solid #E6A23C;
    transition: all 0.3s;

    &.exam-active {
      border-left-color: #409EFF;
      background: #f5f7fa;

      &:hover {
        box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
      }
    }

    .exam-info {
      flex: 1;

      h4 {
        margin: 0 0 8px 0;
        font-size: 15px;
        color: #303133;
      }

      .exam-details {
        color: #909399;
        font-size: 13px;

        .divider {
          margin: 0 8px;
        }
      }
    }

    .exam-action {
      margin-left: 16px;
    }
  }
}

.chart-container {
  height: 500px;

  .chart {
    width: 100%;
    height: 100%;
  }
}

.recent-exam-list {
  min-height: 200px;
  
  .recent-exam-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid #f0f0f0;

    &:last-child {
      border-bottom: none;
    }

    .exam-name {
      color: #606266;
      font-size: 14px;
    }

    .exam-score {
      font-weight: 600;
      font-size: 14px;

      &.score-excellent {
        color: #67C23A;
      }

      &.score-good {
        color: #409EFF;
      }

      &.score-pass {
        color: #E6A23C;
      }

      &.score-fail {
        color: #F56C6C;
      }
    }
  }
}

.empty-state {
  padding: 20px 0;
}

@media (max-width: 1200px) {
  .main-content {
    grid-template-columns: 1fr;
  }

  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
