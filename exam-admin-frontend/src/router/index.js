import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/login/forgot-password.vue'),
    meta: { title: '重置密码' }
  },
  {
    path: '/admin',
    component: () => import('@/layout/admin.vue'),
    redirect: '/admin/dashboard',
    meta: { requiresAuth: true, role: 1 },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/dashboard/index.vue'),
        meta: { title: '数据概览', icon: 'DataAnalysis' }
      },
      {
        path: 'students',
        name: 'AdminStudentManagement',
        component: () => import('@/views/admin/student/index.vue'),
        meta: { title: '学生管理', icon: 'User' }
      },
      {
        path: 'teachers',
        name: 'AdminTeacherManagement',
        component: () => import('@/views/admin/teacher/index.vue'),
        meta: { title: '教师管理', icon: 'Avatar' }
      },
      {
        path: 'departments',
        name: 'AdminDepartmentManagement',
        component: () => import('@/views/admin/department/index.vue'),
        meta: { title: '院系管理', icon: 'School' }
      },
      {
        path: 'classes',
        name: 'AdminClassManagement',
        component: () => import('@/views/admin/class/index.vue'),
        meta: { title: '班级管理', icon: 'Reading' }
      },
      {
        path: 'subject',
        name: 'AdminSubjectManagement',
        component: () => import('@/views/admin/subject/index.vue'),
        meta: { title: '科目管理', icon: 'Collection' }
      },
      {
        path: 'users',
        name: 'AdminUserManagement',
        component: () => import('@/views/admin/users/index.vue'),
        meta: { title: '用户管理', icon: 'UserFilled' }
      },
      {
        path: 'exams',
        name: 'AdminExamManagement',
        component: () => import('@/views/admin/exams/index.vue'),
        meta: { title: '考试管理', icon: 'Timer' }
      },
      {
        path: 'exam-monitor',
        name: 'AdminExamMonitor',
        component: () => import('@/views/admin/exam-monitor/index.vue'),
        meta: { title: '考试监控', icon: 'VideoCamera' }
      },
      {
        path: 'score-stats',
        name: 'AdminScoreStats',
        component: () => import('@/views/admin/score-stats/index.vue'),
        meta: { title: '成绩统计', icon: 'TrendCharts' }
      },
      {
        path: 'settings',
        name: 'AdminSettings',
        component: () => import('@/views/admin/settings/index.vue'),
        meta: { title: '系统设置', icon: 'Setting' }
      },
      {
        path: 'permissions',
        name: 'AdminPermissions',
        component: () => import('@/views/admin/permissions/index.vue'),
        meta: { title: '权限配置', icon: 'Lock' }
      },
      {
        path: 'logs',
        name: 'AdminSystemLogs',
        component: () => import('@/views/admin/logs/index.vue'),
        meta: { title: '系统日志', icon: 'Document' }
      },
      {
        path: 'profile',
        name: 'AdminProfile',
        component: () => import('@/views/admin/profile/index.vue'),
        meta: { title: '个人中心', icon: 'User' }
      },
      {
        path: 'statistics',
        name: 'AdminDataStatistics',
        component: () => import('@/views/admin/statistics/index.vue'),
        meta: { title: '数据统计', icon: 'TrendCharts' }
      }
    ]
  },
  {
    path: '/teacher',
    component: () => import('@/layout/teacher.vue'),
    redirect: '/teacher/dashboard',
    meta: { requiresAuth: true, role: 2 },
    children: [
      {
        path: 'dashboard',
        name: 'TeacherDashboard',
        component: () => import('@/views/teacher/dashboard/index.vue'),
        meta: { title: '工作台', icon: 'DataAnalysis' }
      },
      {
        path: 'question-bank',
        name: 'QuestionBank',
        component: () => import('@/views/teacher/question-bank/index.vue'),
        meta: { title: '题库管理', icon: 'Collection' }
      },
      {
        path: 'paper',
        name: 'PaperManagement',
        component: () => import('@/views/teacher/paper/index.vue'),
        meta: { title: '试卷管理', icon: 'Document' }
      },
      {
        path: 'exam',
        name: 'ExamManagement',
        component: () => import('@/views/teacher/exam/index.vue'),
        meta: { title: '考试管理', icon: 'Timer' }
      },
      {
        path: 'grading',
        name: 'GradingManagement',
        component: () => import('@/views/teacher/grading/index.vue'),
        meta: { title: '阅卷与成绩', icon: 'EditPen' }
      },
      {
        path: 'students',
        name: 'TeacherStudentManagement',
        component: () => import('@/views/teacher/student/index.vue'),
        meta: { title: '学生管理', icon: 'User' }
      },
      {
        path: 'profile',
        name: 'TeacherProfile',
        component: () => import('@/views/teacher/profile/index.vue'),
        meta: { title: '个人中心', icon: 'User' }
      }
    ]
  },
  {
    path: '/student',
    component: () => import('@/layout/student.vue'),
    redirect: '/student/dashboard',
    meta: { requiresAuth: true, role: 3 },
    children: [
      {
        path: 'dashboard',
        name: 'StudentDashboard',
        component: () => import('@/views/student/dashboard/index.vue'),
        meta: { title: '首页', icon: 'HomeFilled' }
      },
      {
        path: 'exam-list',
        name: 'StudentExamList',
        component: () => import('@/views/student/exam-list/index.vue'),
        meta: { title: '我的考试', icon: 'List' }
      },
      {
        path: 'online-exam',
        name: 'OnlineExam',
        component: () => import('@/views/student/online-exam/index.vue'),
        meta: { title: '在线考试', icon: 'Edit' }
      },
      {
        path: 'exam-record',
        name: 'ExamRecord',
        component: () => import('@/views/student/exam-record/index.vue'),
        meta: { title: '历史考试', icon: 'Document' }
      },
      {
        path: 'score-detail',
        name: 'ScoreDetail',
        component: () => import('@/views/student/score-detail/index.vue'),
        meta: { title: '考试详情', requiresAuth: true, role: 3 }
      },
      {
        path: 'wrong-book',
        name: 'WrongBook',
        component: () => import('@/views/student/wrong-book/index.vue'),
        meta: { title: '错题本', icon: 'CollectionTag' }
      },
      {
        path: 'score',
        name: 'ScoreAnalysis',
        component: () => import('@/views/student/score/index.vue'),
        meta: { title: '成绩查询', icon: 'TrendCharts' }
      },
      {
        path: 'profile',
        name: 'StudentProfile',
        component: () => import('@/views/student/profile/index.vue'),
        meta: { title: '个人中心', icon: 'User' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const token = userStore.token
  const userInfo = userStore.userInfo
  
  console.log('[Router] 路由守卫 - to:', to.path)
  console.log('[Router] 路由守卫 - token:', token ? '存在' : '不存在')
  console.log('[Router] 路由守卫 - userInfo:', userInfo)
  
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 在线考试系统` : '在线考试系统'
  
  // 访问登录页或重置密码页
  if (to.path === '/login' || to.path === '/forgot-password') {
    // 如果已经登录，跳转到对应角色的首页
    if (token && userInfo) {
      const roleRoutes = {
        1: '/admin/dashboard',
        2: '/teacher/dashboard',
        3: '/student/dashboard'
      }
      const targetRoute = roleRoutes[userInfo.role]
      console.log('[Router] 已登录，跳转到:', targetRoute)
      if (targetRoute) {
        next(targetRoute)
        return
      }
    }
    next()
    return
  }
  
  // 未登录用户访问其他页面，重定向到登录页
  // 注意：token从localStorage加载，刷新页面也会保留
  if (!token) {
    console.log('[Router] token不存在，跳转到登录页')
    next('/login')
    return
  }
  
  // 已登录但用户信息丢失（异常情况），重新跳转到登录页
  if (!userInfo) {
    console.log('[Router] userInfo不存在，清除token并跳转登录页')
    userStore.logout()
    next('/login')
    return
  }
  
  // 已登录用户，检查角色权限（只检查具体的子路由，不检查父路由的redirect）
  if (to.meta.requiresAuth && to.meta.role && to.matched.length > 1) {
    const userRole = userInfo?.role
    const roleNumber = typeof userRole === 'string' ? parseInt(userRole) : userRole
    const metaRoleNumber = typeof to.meta.role === 'string' ? parseInt(to.meta.role) : to.meta.role
    
    if (roleNumber !== metaRoleNumber) {
      // 角色不匹配，跳转到对应角色的首页
      const roleRoutes = {
        1: '/admin/dashboard',
        2: '/teacher/dashboard',
        3: '/student/dashboard'
      }
      console.log('[Router] 角色不匹配，跳转到:', roleRoutes[userRole])
      next(roleRoutes[userRole] || '/login')
      return
    }
  }
  
  console.log('[Router] 路由验证通过')
  next()
})

export default router
