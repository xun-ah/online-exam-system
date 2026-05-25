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
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Document, TrendCharts, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { getScoreList, getScoreTrend, getKnowledgeRadar } from '@/api/student/index'
import * as echarts from 'echarts'

const router = useRouter()
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
      
      const examNames = res.data.map(item => item.examName)
      const scores = res.data.map(item => item.score)
      
      // 根据分数设置点的颜色
      const itemColors = scores.map(score => {
        if (score >= 90) return '#67c23a'
        if (score >= 80) return '#409eff'
        if (score >= 60) return '#e6a23c'
        return '#f56c6c'
      })
      
      const option = {
        tooltip: {
          trigger: 'axis',
          formatter: function(params) {
            const item = params[0]
            const score = item.value
            let status = score >= 60 ? '及格' : '不及格'
            return `${item.name}<br/>分数: ${score}分<br/>状态: ${status}`
          }
        },
        xAxis: {
          type: 'category',
          data: examNames,
          axisLabel: {
            rotate: 20,
            interval: 0,
            fontSize: 12
          },
          axisLine: {
            lineStyle: {
              color: '#ddd'
            }
          }
        },
        yAxis: {
          type: 'value',
          name: '分数',
          min: 0,
          max: 100,
          axisLine: {
            lineStyle: {
              color: '#ddd'
            }
          },
          splitLine: {
            lineStyle: {
              color: '#f0f0f0',
              type: 'dashed'
            }
          }
        },
        series: [{
          data: scores.map((score, index) => ({
            value: score,
            itemStyle: {
              color: itemColors[index]
            }
          })),
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 8,
          lineStyle: {
            color: '#409eff',
            width: 3
          },
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
        }],
        // 添加及格线
        visualMap: {
          show: false,
          pieces: [{
            gte: 60,
            lte: 100,
            color: '#67c23a'
          }, {
            gte: 0,
            lt: 60,
            color: '#f56c6c'
          }],
          outOfRange: {
            color: '#999'
          }
        },
        markLine: {
          data: [{
            yAxis: 60,
            name: '及格线',
            lineStyle: {
              color: '#f56c6c',
              type: 'dashed',
              width: 2
            },
            label: {
              formatter: '及格线 60分',
              position: 'end',
              color: '#f56c6c',
              fontSize: 12
            }
          }],
          silent: true
        },
        grid: {
          left: '10%',
          right: '5%',
          bottom: '18%',
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
          trigger: 'item',
          formatter: function(params) {
            return `${params.name}: ${params.value}分`
          }
        },
        radar: {
          indicator: res.data.map(item => ({
            name: item.name,
            max: 100
          })),
          radius: '65%',
          // 只显示外圈，隐藏内部同心圆
          splitNumber: 1,
          axisName: {
            color: '#303133',
            fontSize: 13,
            fontWeight: 'bold'
          },
          splitArea: {
            show: false
          },
          axisLine: {
            lineStyle: {
              color: '#ddd'
            }
          },
          splitLine: {
            lineStyle: {
              color: '#eee'
            }
          }
        },
        series: [{
          type: 'radar',
          data: [{
            value: res.data.map(item => item.score),
            name: '掌握度',
            areaStyle: {
              color: {
                type: 'linear',
                x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [
                  { offset: 0, color: 'rgba(64, 158, 255, 0.4)' },
                  { offset: 1, color: 'rgba(64, 158, 255, 0.1)' }
                ]
              }
            },
            lineStyle: {
              color: '#409eff',
              width: 2
            },
            itemStyle: {
              color: '#409eff',
              borderColor: '#fff',
              borderWidth: 2
            },
            // 显示每个顶点的分数标签
            label: {
              show: true,
              formatter: function(params) {
                return params.value
              },
              color: '#409eff',
              fontSize: 12,
              fontWeight: 'bold',
              position: 'top'
            }
          }]
        }]
      }
      chart.setOption(option)
      window.addEventListener('resize', () => chart.resize())
    } else if (radarChartRef.value) {
      // 没有数据时显示提示
      const chart = echarts.init(radarChartRef.value)
      const option = {
        graphic: {
          type: 'text',
          left: 'center',
          top: 'center',
          style: {
            text: '暂无考试数据',
            fontSize: 14,
            fill: '#909399'
          }
        }
      }
      chart.setOption(option)
    }
  } catch (error) {
    console.error('获取知识点数据失败:', error)
  }
}

// 查看详情
const viewDetail = (row) => {
  router.push({
    name: 'ScoreDetail',
    query: { recordId: row.id }
  })
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
