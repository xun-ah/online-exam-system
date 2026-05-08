<template>
  <div class="score-container">
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item>首页</el-breadcrumb-item>
      <el-breadcrumb-item>成绩查询</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- 成绩统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #409eff;">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ scoreList.length }}</div>
              <div class="stat-label">考试次数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #67c23a;">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ avgScore }}</div>
              <div class="stat-label">平均分</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #e6a23c;">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ maxScore }}</div>
              <div class="stat-label">最高分</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #f56c6c;">
              <el-icon><CircleClose /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ minScore }}</div>
              <div class="stat-label">最低分</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 成绩列表 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>成绩列表</span>
        </div>
      </template>
      <el-table :data="scoreList" v-loading="loading" stripe>
        <el-table-column prop="examName" label="考试名称" min-width="200" />
        <el-table-column prop="score" label="得分" width="120">
          <template #default="{row}">
            <el-tag :type="getScoreType(row.score)" size="large">
              {{ row.score }}分
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{row}">
            <el-tag :type="row.score >= 60 ? 'success' : 'danger'">
              {{ row.score >= 60 ? '及格' : '不及格' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="180">
          <template #default="{row}">
            {{ formatTime(row.submitTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="用时" width="100">
          <template #default="{row}">
            {{ row.duration }}分钟
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{row}">
            <el-button type="primary" size="small" @click="viewDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && scoreList.length === 0" description="暂无成绩记录" />
    </el-card>

    <!-- 成绩趋势图表 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <span>成绩趋势</span>
          </template>
          <div ref="trendChartRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <span>知识点掌握情况</span>
          </template>
          <div ref="radarChartRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, TrendCharts, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { getScoreList, getScoreTrend, getKnowledgeRadar } from '@/api/student/index'
import * as echarts from 'echarts'

const loading = ref(false)
const scoreList = ref([])
const trendChartRef = ref(null)
const radarChartRef = ref(null)

// 计算统计数据
const avgScore = computed(() => {
  if (scoreList.value.length === 0) return 0
  const sum = scoreList.value.reduce((acc, item) => acc + (item.score || 0), 0)
  return (sum / scoreList.value.length).toFixed(1)
})

const maxScore = computed(() => {
  if (scoreList.value.length === 0) return 0
  return Math.max(...scoreList.value.map(item => item.score || 0))
})

const minScore = computed(() => {
  if (scoreList.value.length === 0) return 0
  return Math.min(...scoreList.value.map(item => item.score || 0))
})

// 获取成绩列表
const fetchScoreList = async () => {
  loading.value = true
  try {
    const res = await getScoreList()
    if (res.data) {
      scoreList.value = res.data
    }
  } catch (error) {
    console.error('获取成绩列表失败:', error)
    ElMessage.error('获取成绩列表失败')
  } finally {
    loading.value = false
  }
}

// 初始化成绩趋势图表
const initTrendChart = async () => {
  try {
    const res = await getScoreTrend()
    if (res.data && res.data.length > 0 && trendChartRef.value) {
      const chart = echarts.init(trendChartRef.value)
      const option = {
        tooltip: {
          trigger: 'axis',
          formatter: '{b}: {c}分'
        },
        xAxis: {
          type: 'category',
          data: res.data.map(item => item.examName),
          axisLabel: {
            rotate: 30,
            interval: 0
          }
        },
        yAxis: {
          type: 'value',
          name: '分数'
        },
        series: [{
          data: res.data.map(item => item.score),
          type: 'line',
          smooth: true,
          lineStyle: {
            color: '#409eff',
            width: 3
          },
          itemStyle: {
            color: '#409eff'
          },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
                { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
              ]
            }
          }
        }],
        grid: {
          left: '10%',
          right: '5%',
          bottom: '15%',
          top: '10%'
        }
      }
      chart.setOption(option)
      window.addEventListener('resize', () => chart.resize())
    }
  } catch (error) {
    console.error('获取成绩趋势失败:', error)
  }
}

// 初始化雷达图
const initRadarChart = async () => {
  try {
    const res = await getKnowledgeRadar()
    if (res.data && res.data.length > 0 && radarChartRef.value) {
      const chart = echarts.init(radarChartRef.value)
      const option = {
        tooltip: {
          trigger: 'item'
        },
        radar: {
          indicator: res.data.map(item => ({
            name: item.name,
            max: 100
          })),
          radius: '65%'
        },
        series: [{
          type: 'radar',
          data: [{
            value: res.data.map(item => item.score),
            name: '掌握度',
            areaStyle: {
              color: 'rgba(64, 158, 255, 0.3)'
            },
            lineStyle: {
              color: '#409eff',
              width: 2
            },
            itemStyle: {
              color: '#409eff'
            }
          }]
        }]
      }
      chart.setOption(option)
      window.addEventListener('resize', () => chart.resize())
    }
  } catch (error) {
    console.error('获取知识点数据失败:', error)
  }
}

// 查看详情
const viewDetail = (row) => {
  ElMessage.info(`查看考试 ${row.examName} 的详情`)
  // TODO: 跳转到考试详情页面
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取分数标签类型
const getScoreType = (score) => {
  if (score >= 90) return 'success'
  if (score >= 80) return ''
  if (score >= 60) return 'warning'
  return 'danger'
}

onMounted(() => {
  fetchScoreList()
  initTrendChart()
  initRadarChart()
})
</script>

<style scoped lang="scss">
.score-container {
  padding: 20px;
  
  .breadcrumb {
    margin-bottom: 20px;
  }
  
  .stats-row {
    margin-bottom: 20px;
    
    .stat-card {
      border-radius: 8px;
      
      .stat-content {
        display: flex;
        align-items: center;
        gap: 15px;
        
        .stat-icon {
          width: 50px;
          height: 50px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: #fff;
          font-size: 24px;
        }
        
        .stat-info {
          .stat-value {
            font-size: 28px;
            font-weight: bold;
            color: #303133;
            margin-bottom: 5px;
          }
          
          .stat-label {
            font-size: 14px;
            color: #909399;
          }
        }
      }
    }
  }
  
  .table-card {
    border-radius: 8px;
    margin-bottom: 20px;
    
    .card-header {
      font-size: 16px;
      font-weight: bold;
    }
  }
  
  .chart-row {
    margin-bottom: 20px;
    
    .el-card {
      border-radius: 8px;
    }
  }
}
</style>
