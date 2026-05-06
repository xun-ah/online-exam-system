<template>
  <div class="dashboard-container">
    <!-- 面包屑导航 -->
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>仪表盘</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-cards">
      <el-col :span="6">
        <div class="stat-card stat-blue">
          <div class="stat-bar"></div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.totalStudents.toLocaleString() }}</div>
            <div class="stat-label">总学生数</div>
            <div class="stat-trend">较上月 <span class="trend-up">{{ stats.studentGrowth }}</span></div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-green">
          <div class="stat-bar"></div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.totalTeachers }}</div>
            <div class="stat-label">总教师数</div>
            <div class="stat-trend">较上月 <span class="trend-up">{{ stats.teacherGrowth }}</span></div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-orange">
          <div class="stat-bar"></div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.monthlyExams }}</div>
            <div class="stat-label">本月考试</div>
            <div class="stat-trend">较上月 <span class="trend-up">{{ stats.examGrowth }}</span></div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-red">
          <div class="stat-bar"></div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.totalParticipants.toLocaleString() }}</div>
            <div class="stat-label">参与人次</div>
            <div class="stat-trend">较上月 <span class="trend-up">{{ stats.participantGrowth }}</span></div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 快捷操作 -->
    <div class="section-title">快捷操作</div>
    <el-row :gutter="16" class="quick-actions">
      <el-col :span="6">
        <el-button type="primary" class="action-btn" @click="navigateTo('/admin/students')">导入学生</el-button>
      </el-col>
      <el-col :span="6">
        <el-button type="purple" class="action-btn action-purple" @click="navigateTo('/admin/teachers')">导入教师</el-button>
      </el-col>
      <el-col :span="6">
        <el-button type="success" class="action-btn action-green" @click="navigateTo('/admin/classes')">班级管理</el-button>
      </el-col>
      <el-col :span="6">
        <el-button type="warning" class="action-btn action-orange" @click="navigateTo('/admin/departments')">院系管理</el-button>
      </el-col>
    </el-row>

    <!-- 系统操作日志 -->
    <el-card class="logs-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">系统操作日志</span>
          <el-input
            v-model="searchText"
            placeholder="搜索操作记录..."
            class="search-input"
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
      </template>

      <el-table :data="logData" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="time" label="操作时间" width="180" />
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="type" label="操作类型" width="120" />
        <el-table-column prop="detail" label="操作详情" min-width="200" />
        <el-table-column prop="ip" label="IP地址" width="150" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <span :class="['status-tag', scope.row.status === '成功' ? 'status-success' : 'status-warning']">
              {{ scope.row.status }}
            </span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          layout="prev, pager, next"
          :total="total"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { getDashboardStats, getSystemLogs } from '@/api/admin'
import { ElMessage } from 'element-plus'

const router = useRouter()
const searchText = ref('')
const currentPage = ref(1)
const pageSize = ref(8)
const total = ref(0)

// 统计数据
const stats = ref({
  totalStudents: 0,
  totalTeachers: 0,
  monthlyExams: 0,
  totalParticipants: 0,
  studentGrowth: '+0%',
  teacherGrowth: '+0%',
  examGrowth: '+0%',
  participantGrowth: '+0%'
})

// 日志数据
const logData = ref([])
const loading = ref(false)

// 快捷操作跳转
const navigateTo = (path) => {
  router.push(path)
}

// 获取统计数据
const fetchStats = async () => {
  try {
    const res = await getDashboardStats()
    stats.value = res.data
  } catch (error) {
    console.error('获取统计数据失败:', error)
    ElMessage.error('获取统计数据失败')
  }
}

// 获取日志数据
const fetchLogs = async () => {
  loading.value = true
  try {
    const res = await getSystemLogs({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      keyword: searchText.value
    })
    logData.value = res.data.list || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error('获取日志数据失败:', error)
    ElMessage.error('获取日志数据失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchLogs()
}

// 分页变化
const handlePageChange = (page) => {
  currentPage.value = page
  fetchLogs()
}

onMounted(() => {
  fetchStats()
  fetchLogs()
})
</script>

<style scoped lang="scss">
.dashboard-container {
  padding: 0;
  height: calc(100vh - 56px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.breadcrumb {
  margin-bottom: 16px;
  font-size: 14px;
}

// 统计卡片
.stats-cards {
  margin-bottom: 16px;
  
  .stat-card {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    position: relative;
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    
    .stat-bar {
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 4px;
    }
    
    .stat-content {
      text-align: center;
      
      .stat-value {
        font-size: 32px;
        font-weight: 700;
        margin-bottom: 8px;
      }
      
      .stat-label {
        font-size: 14px;
        color: #8c8c8c;
        margin-bottom: 8px;
      }
      
      .stat-trend {
        font-size: 12px;
        color: #8c8c8c;
        
        .trend-up {
          color: #52c41a;
        }
      }
    }
    
    &.stat-blue {
      .stat-bar {
        background: #1890ff;
      }
      .stat-value {
        color: #1890ff;
      }
    }
    
    &.stat-green {
      .stat-bar {
        background: #52c41a;
      }
      .stat-value {
        color: #52c41a;
      }
    }
    
    &.stat-orange {
      .stat-bar {
        background: #faad14;
      }
      .stat-value {
        color: #faad14;
      }
    }
    
    &.stat-red {
      .stat-bar {
        background: #f5222d;
      }
      .stat-value {
        color: #f5222d;
      }
    }
  }
}

// 快捷操作
.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 12px;
}

.quick-actions {
  margin-bottom: 16px;
  
  .action-btn {
    width: 100%;
    height: 40px;
    font-size: 14px;
    border-radius: 6px;
    
    &.action-purple {
      background: #722ed1;
      border-color: #722ed1;
      color: #fff;
      
      &:hover {
        background: #9254de;
        border-color: #9254de;
      }
    }
    
    &.action-green {
      background: #52c41a;
      border-color: #52c41a;
      color: #fff;
      
      &:hover {
        background: #73d13d;
        border-color: #73d13d;
      }
    }
    
    &.action-orange {
      background: #fa8c16;
      border-color: #fa8c16;
      color: #fff;
      
      &:hover {
        background: #ffa940;
        border-color: #ffa940;
      }
    }
  }
}

// 日志卡片
.logs-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  margin-top: 16px;
  
  :deep(.el-card__header) {
    padding: 16px 20px;
    border-bottom: 1px solid #f0f0f0;
    flex-shrink: 0;
  }
  
  :deep(.el-card__body) {
    padding: 20px;
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .card-title {
      font-size: 16px;
      font-weight: 600;
      color: #262626;
    }
    
    .search-input {
      width: 240px;
    }
  }
  
  .status-tag {
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 12px;
    
    &.status-success {
      background: #f6ffed;
      color: #52c41a;
      border: 1px solid #b7eb8f;
    }
    
    &.status-warning {
      background: #fff7e6;
      color: #fa8c16;
      border: 1px solid #ffd591;
    }
  }
  
  .el-table {
    flex: 1;
    overflow: auto;
  }
  
  :deep(.el-table__body-wrapper) {
    overflow-y: auto;
  }
  
  .pagination {
    display: flex;
    justify-content: center;
    margin-top: auto;
    padding-top: 16px;
    flex-shrink: 0;
  }
}
</style>
