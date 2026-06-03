<template>
  <div class="page-container">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-cards">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon blue">
              <el-icon :size="40"><User /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ dashboardStats.totalStudents || 0 }}</div>
              <div class="stat-label">学生总数</div>
              <div class="stat-growth">{{ dashboardStats.studentGrowth || '+0%' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon green">
              <el-icon :size="40"><UserFilled /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ dashboardStats.totalTeachers || 0 }}</div>
              <div class="stat-label">教师总数</div>
              <div class="stat-growth">{{ dashboardStats.teacherGrowth || '+0%' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon orange">
              <el-icon :size="40"><Document /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ dashboardStats.monthlyExams || 0 }}</div>
              <div class="stat-label">本月考试场次</div>
              <div class="stat-growth">{{ dashboardStats.examGrowth || '+0%' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon red">
              <el-icon :size="40"><TrendCharts /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ dashboardStats.totalParticipants || 0 }}</div>
              <div class="stat-label">总参与人次</div>
              <div class="stat-growth">{{ dashboardStats.participantGrowth || '+0%' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 图表区域 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>各院系考试参与情况</span>
            </div>
          </template>
          <div ref="departmentChartRef" style="height: 400px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>题型分布统计</span>
            </div>
          </template>
          <div ref="questionTypeChartRef" style="height: 400px;"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" class="chart-row">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>月度考试趋势</span>
            </div>
          </template>
          <div ref="monthlyTrendChartRef" style="height: 400px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import * as echarts from 'echarts'
import { getDashboardStats, getDepartmentExamStats, getQuestionTypeStats, getMonthlyTrend } from '@/api/admin'

const dashboardStats = reactive({
  totalStudents: 0,
  totalTeachers: 0,
  monthlyExams: 0,
  totalParticipants: 0,
  studentGrowth: '+0%',
  teacherGrowth: '+0%',
  examGrowth: '+0%',
  participantGrowth: '+0%'
})

const departmentChartRef = ref(null)
const questionTypeChartRef = ref(null)
const monthlyTrendChartRef = ref(null)

let departmentChart = null
let questionTypeChart = null
let monthlyTrendChart = null

// 获取仪表盘数据
const fetchDashboardStats = async () => {
  try {
    const res = await getDashboardStats()
    Object.assign(dashboardStats, res.data)
  } catch (error) {
    console.error('获取仪表盘数据失败:', error)
  }
}

// 获取院系考试数据
const fetchDepartmentExamStats = async () => {
  try {
    const res = await getDepartmentExamStats()
    if (departmentChart) {
      const data = res.data || []
      departmentChart.setOption({
        xAxis: {
          data: data.map(item => item.name)
        },
        series: [{
          data: data.map(item => item.count)
        }]
      })
    }
  } catch (error) {
    console.error('获取院系考试数据失败:', error)
  }
}

// 获取题型分布数据
const fetchQuestionTypeStats = async () => {
  try {
    const res = await getQuestionTypeStats()
    if (questionTypeChart) {
      const data = res.data || []
      questionTypeChart.setOption({
        series: [{
          data: data.map(item => ({
            name: item.name,
            value: item.count
          }))
        }]
      })
    }
  } catch (error) {
    console.error('获取题型分布数据失败:', error)
  }
}

// 获取月度趋势数据
const fetchMonthlyTrend = async () => {
  try {
    const res = await getMonthlyTrend()
    if (monthlyTrendChart) {
      const data = res.data || []
      monthlyTrendChart.setOption({
        xAxis: {
          data: data.map(item => item.month)
        },
        series: [{
          data: data.map(item => item.count)
        }]
      })
    }
  } catch (error) {
    console.error('获取月度趋势数据失败:', error)
  }
}

// 初始化图表
const initCharts = () => {
  // 院系考试柱状图
  if (departmentChartRef.value) {
    departmentChart = echarts.init(departmentChartRef.value)
    departmentChart.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' }
      },
      xAxis: {
        type: 'category',
        data: [],
        axisLabel: { interval: 0 }
      },
      yAxis: {
        type: 'value'
      },
      series: [{
        type: 'bar',
        data: [],
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#83bff6' },
            { offset: 0.5, color: '#188df0' },
            { offset: 1, color: '#188df0' }
          ])
        },
        emphasis: {
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#2378f7' },
              { offset: 0.7, color: '#2378f7' },
              { offset: 1, color: '#83bff6' }
            ])
          }
        }
      }],
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      }
    })
  }
  
  // 题型分布饼图
  if (questionTypeChartRef.value) {
    questionTypeChart = echarts.init(questionTypeChartRef.value)
    questionTypeChart.setOption({
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b}: {c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        right: 10,
        top: 'center'
      },
      series: [{
        name: '题型分布',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}: {d}%'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        data: []
      }]
    })
  }
  
  // 月度趋势折线图
  if (monthlyTrendChartRef.value) {
    monthlyTrendChart = echarts.init(monthlyTrendChartRef.value)
    monthlyTrendChart.setOption({
      tooltip: {
        trigger: 'axis'
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: []
      },
      yAxis: {
        type: 'value'
      },
      series: [{
        name: '考试场次',
        type: 'line',
        data: [],
        smooth: true,
        itemStyle: {
          color: '#1890ff'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(24,144,255,0.3)' },
            { offset: 1, color: 'rgba(24,144,255,0.05)' }
          ])
        }
      }],
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      }
    })
  }
}

// 响应式调整
const handleResize = () => {
  departmentChart?.resize()
  questionTypeChart?.resize()
  monthlyTrendChart?.resize()
}

onMounted(async () => {
  // 初始化图表
  initCharts()
  
  // 获取数据
  await Promise.all([
    fetchDashboardStats(),
    fetchDepartmentExamStats(),
    fetchQuestionTypeStats(),
    fetchMonthlyTrend()
  ])
  
  // 监听窗口大小变化
  window.addEventListener('resize', handleResize)
})
</script>

<style scoped lang="scss">
.page-container {
  padding: 20px;
}

.stats-cards {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 20px;
  
  .stat-icon {
    width: 80px;
    height: 80px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    
    &.blue {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
    }
    
    &.green {
      background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
      color: white;
    }
    
    &.orange {
      background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
      color: white;
    }
    
    &.red {
      background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
      color: white;
    }
  }
  
  .stat-content {
    flex: 1;
    
    .stat-value {
      font-size: 28px;
      font-weight: bold;
      color: #303133;
      margin-bottom: 5px;
    }
    
    .stat-label {
      font-size: 14px;
      color: #909399;
      margin-bottom: 5px;
    }
    
    .stat-growth {
      font-size: 12px;
      color: #67c23a;
    }
  }
}

.chart-row {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
