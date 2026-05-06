<template>
  <div class="student-layout">
    <!-- 顶部导航栏 -->
    <div class="top-header">
      <div class="header-left">
        <!-- 折叠按钮 -->
        <el-button class="collapse-btn" @click="toggleSidebar" :title="isCollapsed ? '展开侧边栏' : '收起侧边栏'">
          <el-icon><component :is="isCollapsed ? 'Expand' : 'Fold'" /></el-icon>
        </el-button>
        <div class="logo">在线考试系统</div>
        <!-- 面包屑导航 -->
        <el-breadcrumb separator="/" class="breadcrumb">
          <el-breadcrumb-item :to="{ path: '/student/dashboard' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item v-if="route.meta.title">
            {{ route.meta.title }}
          </el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <div class="header-right">
        <div class="right-actions">
          <!-- 全屏切换 -->
          <el-button class="icon-btn" @click="toggleFullscreen" title="全屏切换">
            <el-icon><FullScreen /></el-icon>
          </el-button>
          <!-- 刷新 -->
          <el-button class="icon-btn" @click="refreshPage" title="刷新页面">
            <el-icon><Refresh /></el-icon>
          </el-button>
          <!-- 用户信息 -->
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><User /></el-icon>
              <span>{{ userInfo?.realName || userInfo?.username || '学生' }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>

    <div class="main-container">
      <!-- 左侧菜单 -->
      <div class="sidebar" :class="{ collapsed: isCollapsed }">
        <el-menu
          :default-active="activeMenu"
          router
          class="side-menu"
          background-color="#001529"
          text-color="rgba(255, 255, 255, 0.85)"
          active-text-color="#1890ff"
          :default-openeds="['/student/dashboard']"
          :collapse="isCollapsed"
        >
          <el-menu-item index="/student/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <template #title>首页</template>
          </el-menu-item>
          <el-menu-item index="/student/exam-list">
            <el-icon><Timer /></el-icon>
            <template #title>待考考试</template>
          </el-menu-item>
          <el-menu-item index="/student/exam-record">
            <el-icon><Document /></el-icon>
            <template #title>历史考试</template>
          </el-menu-item>
          <el-menu-item index="/student/score">
            <el-icon><TrendCharts /></el-icon>
            <template #title>成绩查询</template>
          </el-menu-item>
          <el-menu-item index="/student/wrong-book">
            <el-icon><CollectionTag /></el-icon>
            <template #title>错题本</template>
          </el-menu-item>
          <el-menu-item index="/student/exam-record">
            <el-icon><Tickets /></el-icon>
            <template #title>成绩单</template>
          </el-menu-item>
          <el-menu-item index="/student/profile">
            <el-icon><User /></el-icon>
            <template #title>个人信息</template>
          </el-menu-item>
          <el-menu-item index="/student/profile">
            <el-icon><Lock /></el-icon>
            <template #title>修改密码</template>
          </el-menu-item>
        </el-menu>
      </div>

      <!-- 主内容区 -->
      <div class="main-content" :class="{ expanded: isCollapsed }">
        <router-view :key="route.fullPath" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessageBox } from 'element-plus'
import { User, SwitchButton, FullScreen, Refresh, Fold, Expand, DataAnalysis, Timer, Document, TrendCharts, CollectionTag, Tickets, Lock } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 侧边栏折叠状态
const isCollapsed = ref(false)

const activeMenu = computed(() => route.path)

const userInfo = computed(() => userStore.userInfo)

// 切换侧边栏
const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}

// 全屏切换
const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

// 刷新页面
const refreshPage = () => {
  window.location.reload()
}

const handleCommand = (command) => {
  if (command === 'profile') {
    router.push('/student/profile')
  } else if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      userStore.logout()
      router.replace('/login')
    }).catch(() => {
      // 用户取消
    })
  }
}
</script>

<style scoped lang="scss">
.student-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f0f2f5;
}

// 顶部导航栏
.top-header {
  height: 56px;
  background-color: #001529;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  
  .header-left {
    display: flex;
    align-items: center;
    flex: 1;
    
    .collapse-btn {
      color: rgba(255, 255, 255, 0.85);
      background: transparent;
      border: none;
      padding: 8px;
      margin-right: 12px;
      
      &:hover {
        color: #1890ff;
        background: rgba(24, 144, 255, 0.1);
      }
    }
    
    .logo {
      color: #fff;
      font-size: 18px;
      font-weight: 600;
      margin-right: 24px;
      white-space: nowrap;
    }
    
    .breadcrumb {
      :deep(.el-breadcrumb__item) {
        .el-breadcrumb__inner {
          color: rgba(255, 255, 255, 0.65);
          
          &.is-link:hover {
            color: #1890ff;
          }
        }
        
        &:last-child {
          .el-breadcrumb__inner {
            color: rgba(255, 255, 255, 0.85);
          }
        }
      }
    }
  }
  
  .header-right {
    .right-actions {
      display: flex;
      align-items: center;
      gap: 8px;
      height: 100%;
      padding-right: 2px;
      
      .icon-btn {
        color: rgba(255, 255, 255, 0.85);
        background: transparent;
        border: none;
        padding: 8px;
        
        &:hover {
          color: #1890ff;
          background: rgba(24, 144, 255, 0.1);
        }
      }
    }
    
    .user-info {
      color: rgba(255, 255, 255, 0.85);
      cursor: pointer;
      font-size: 14px;
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 6px 12px;
      border-radius: 4px;
      transition: all 0.3s;
      
      &:hover {
        background: rgba(255, 255, 255, 0.1);
      }
    }
  }
}

// 主容器
.main-container {
  flex: 1;
  display: flex;
  overflow: hidden;
}

// 左侧菜单
.sidebar {
  width: 200px;
  background-color: #001529;
  overflow-y: auto;
  transition: width 0.3s;
  
  &.collapsed {
    width: 64px;
  }
  
  .side-menu {
    border-right: none;
    
    :deep(.el-menu-item) {
      height: 48px;
      line-height: 48px;
      font-size: 14px;
      
      &:hover {
        background-color: rgba(24, 144, 255, 0.1);
      }
      
      &.is-active {
        background-color: #1890ff;
        color: #fff;
      }
    }
  }
}

// 主内容区
.main-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background-color: #f0f2f5;
  transition: margin-left 0.3s;
  
  &.expanded {
    margin-left: 0;
  }
}
</style>
