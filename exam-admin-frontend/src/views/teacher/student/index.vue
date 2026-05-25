<template>
  <div class="student-management-container">
    <!-- 面包屑导航 -->
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item :to="{ path: '/teacher/dashboard' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>学生管理</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="院系">
          <el-select v-model="searchForm.departmentId" placeholder="全部院系" clearable style="width: 150px" @change="handleDepartmentChange">
            <el-option
              v-for="dept in departmentOptions"
              :key="dept.id"
              :label="dept.deptName"
              :value="dept.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="searchForm.classId" placeholder="全部班级" clearable style="width: 150px">
            <el-option
              v-for="cls in classOptions"
              :key="cls.id"
              :label="cls.className"
              :value="cls.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="学生姓名">
          <el-input v-model="searchForm.studentName" placeholder="搜索学生姓名" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="学号">
          <el-input v-model="searchForm.studentNo" placeholder="搜索学号" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作按钮 -->
    <el-card class="action-card" shadow="never">
      <div class="action-bar">
        <div class="left-buttons">
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增学生
          </el-button>
          <el-button type="success" @click="handleExport">
            <el-icon><Download /></el-icon>
            导出成绩
          </el-button>
        </div>
        <div class="right-info">
          共 {{ total }} 名学生
        </div>
      </div>
    </el-card>

    <!-- 学生列表 -->
    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="studentList" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="studentNo" label="学号" width="150" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column label="性别" width="80">
          <template #default="{ row }">
            {{ row.gender === 1 ? '男' : '女' }}
          </template>
        </el-table-column>
        <el-table-column prop="className" label="班级" width="150" />
        <el-table-column prop="departmentName" label="院系" width="180" />
        <el-table-column prop="phone" label="联系电话" width="150" />
        <el-table-column prop="email" label="邮箱" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link size="small" @click="handleViewScores(row)">成绩</el-button>
            <el-button type="primary" link size="small" @click="handleViewDetail(row)">详情</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 学生成绩对话框 -->
    <el-dialog v-model="scoreDialogVisible" :title="`${currentStudent.realName} - 考试成绩`" width="900px">
      <el-table :data="scoreList" style="width: 100%">
        <el-table-column prop="examName" label="考试名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="subject" label="科目" width="120" />
        <el-table-column prop="score" label="成绩" width="100">
          <template #default="{ row }">
            <span :style="{ color: row.score >= 60 ? '#67c23a' : '#f56c6c', fontWeight: 'bold' }">
              {{ row.score }}分
            </span>
          </template>
        </el-table-column>
        <el-table-column label="排名" width="100">
          <template #default="{ row }">
            第{{ row.rank }}名
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="180" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 2 ? 'success' : 'warning'" size="small">
              {{ row.status === 2 ? '已阅卷' : '待阅卷' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 学生详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="学生详情" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="学号">{{ currentStudent.studentNo }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ currentStudent.realName }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ currentStudent.gender === 1 ? '男' : '女' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentStudent.phone }}</el-descriptions-item>
        <el-descriptions-item label="邮箱" :span="2">{{ currentStudent.email }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ currentStudent.className }}</el-descriptions-item>
        <el-descriptions-item label="院系">{{ currentStudent.departmentName }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 新增/编辑学生对话框 -->
    <el-dialog v-model="formDialogVisible" :title="formDialogTitle" width="600px">
      <el-alert v-if="!isEdit" title="提示" type="info" :closable="false" style="margin-bottom: 20px;">
        <template #default>
          <p style="margin: 0;">新增学生账号的<strong>默认密码为：123456</strong>，请通知学生首次登录后及时修改密码。</p>
        </template>
      </el-alert>
      <el-form :model="studentForm" :rules="rules" ref="studentFormRef" label-width="100px">
        <el-form-item label="学号" prop="studentNo">
          <el-input v-model="studentForm.studentNo" :disabled="isEdit" placeholder="请输入学号" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="studentForm.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="studentForm.gender">
            <el-radio :label="1">男</el-radio>
            <el-radio :label="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="院系" prop="departmentId">
          <el-select v-model="studentForm.departmentId" placeholder="请选择院系" style="width: 100%" @change="handleFormDepartmentChange">
            <el-option
              v-for="dept in departmentOptions"
              :key="dept.id"
              :label="dept.deptName || dept.name"
              :value="dept.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="班级" prop="classId">
          <el-select v-model="studentForm.classId" placeholder="请选择班级" style="width: 100%">
            <el-option
              v-for="cls in formClassOptions"
              :key="cls.id"
              :label="cls.className || cls.name"
              :value="cls.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="studentForm.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="studentForm.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleFormSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Plus } from '@element-plus/icons-vue'
import { 
  getStudentList, 
  getStudentScores, 
  exportScores, 
  getTeacherDepartment, 
  getClassListByDepartment,
  addStudent,
  updateStudent,
  deleteStudent
} from '@/api/teacher/student'

const loading = ref(false)
const studentList = ref([])
const total = ref(0)
const scoreDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const formDialogVisible = ref(false)
const isEdit = ref(false)
const formDialogTitle = ref('')
const studentFormRef = ref(null)
const departmentOptions = ref([])
const classOptions = ref([])
const formClassOptions = ref([])

const currentStudent = reactive({
  id: null,
  studentNo: '',
  realName: '',
  gender: 1,
  phone: '',
  email: '',
  className: '',
  departmentName: ''
})

const searchForm = reactive({
  departmentId: null,
  classId: null,
  studentName: '',
  studentNo: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10
})

const scoreList = ref([])

const studentForm = reactive({
  id: null,
  studentNo: '',
  realName: '',
  gender: 1,
  departmentId: null,
  classId: null,
  phone: '',
  email: ''
})

const rules = {
  studentNo: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    { pattern: /^[0-9]+$/, message: '学号只能为数字', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  departmentId: [{ required: true, message: '请选择院系', trigger: 'change' }],
  classId: [{ required: true, message: '请选择班级', trigger: 'change' }],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

// 获取教师所属院系
const fetchTeacherDepartment = async () => {
  try {
    const res = await getTeacherDepartment()
    if (res.data) {
      // 设置当前教师的院系
      departmentOptions.value = [res.data]
      searchForm.departmentId = res.data.id
      // 加载该院系的班级
      fetchClasses(res.data.id)
    }
  } catch (error) {
    console.error('获取教师院系信息失败:', error)
  }
}

// 获取班级列表
const fetchClasses = async (departmentId) => {
  try {
    const res = await getClassListByDepartment({ departmentId })
    classOptions.value = res.data || []
  } catch (error) {
    console.error('获取班级列表失败:', error)
  }
}

// 院系变化时更新班级选项
const handleDepartmentChange = (departmentId) => {
  searchForm.classId = null
  classOptions.value = []
  if (departmentId) {
    fetchClasses(departmentId)
  }
  // 重新加载学生列表
  fetchStudents()
}

// 获取学生列表
const fetchStudents = async () => {
  loading.value = true
  try {
    const res = await getStudentList({ 
      ...searchForm, 
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    studentList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error('获取学生列表失败:', error)
    ElMessage.error('获取学生列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1
  fetchStudents()
}

// 重置
const handleReset = () => {
  Object.assign(searchForm, {
    departmentId: null,
    classId: null,
    studentName: '',
    studentNo: ''
  })
  classOptions.value = []
  handleSearch()
}

// 查看成绩
const handleViewScores = async (row) => {
  Object.assign(currentStudent, row)
  try {
    const res = await getStudentScores(row.id)
    scoreList.value = res.data || []
    scoreDialogVisible.value = true
  } catch (error) {
    console.error('获取成绩失败:', error)
    ElMessage.error('获取成绩失败')
  }
}

// 查看详情
const handleViewDetail = (row) => {
  Object.assign(currentStudent, row)
  detailDialogVisible.value = true
}

// 导出成绩
const handleExport = async () => {
  try {
    const res = await exportScores({ classId: searchForm.classId })
    
    // 处理blob响应，创建下载链接
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '学生成绩_' + new Date().getTime() + '.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}

// 新增学生
const handleAdd = () => {
  isEdit.value = false
  formDialogTitle.value = '新增学生'
  resetForm()
  studentForm.departmentId = searchForm.departmentId
  if (searchForm.departmentId) {
    fetchFormClasses(searchForm.departmentId)
  }
  formDialogVisible.value = true
}

// 编辑学生
const handleEdit = (row) => {
  isEdit.value = true
  formDialogTitle.value = '编辑学生'
  Object.assign(studentForm, {
    id: row.id,
    studentNo: row.studentNo,
    realName: row.realName,
    gender: row.gender,
    departmentId: row.departmentId,
    classId: row.classId,
    phone: row.phone,
    email: row.email
  })
  
  // 加载该院系的班级列表
  if (row.departmentId) {
    fetchFormClasses(row.departmentId)
  }
  
  formDialogVisible.value = true
}

// 删除学生
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除学生“${row.realName}”吗？删除后无法恢复。`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await deleteStudent(row.id)
    ElMessage.success('删除成功')
    fetchStudents()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 表单院系变化
const handleFormDepartmentChange = (departmentId) => {
  studentForm.classId = null
  formClassOptions.value = []
  if (departmentId) {
    fetchFormClasses(departmentId)
  }
}

// 获取表单班级列表
const fetchFormClasses = async (departmentId) => {
  try {
    const res = await getClassListByDepartment({ departmentId })
    formClassOptions.value = res.data || []
  } catch (error) {
    console.error('获取班级列表失败:', error)
  }
}

// 重置表单
const resetForm = () => {
  Object.assign(studentForm, {
    id: null,
    studentNo: '',
    realName: '',
    gender: 1,
    departmentId: null,
    classId: null,
    phone: '',
    email: ''
  })
  if (studentFormRef.value) {
    studentFormRef.value.resetFields()
  }
}

// 表单提交
const handleFormSubmit = async () => {
  if (!studentFormRef.value) return
  
  try {
    await studentFormRef.value.validate()
    
    if (isEdit.value) {
      await updateStudent(studentForm.id, studentForm)
      ElMessage.success('更新成功')
    } else {
      await addStudent(studentForm)
      ElMessage.success('新增成功')
    }
    
    formDialogVisible.value = false
    fetchStudents()
  } catch (error) {
    if (error !== false) {
      console.error('提交失败:', error)
      ElMessage.error('提交失败')
    }
  }
}

// 分页变化
const handleSizeChange = (size) => {
  pagination.pageSize = size
  fetchStudents()
}

const handlePageChange = (page) => {
  pagination.pageNum = page
  fetchStudents()
}

onMounted(() => {
  fetchTeacherDepartment()
  fetchStudents()
})
</script>

<style scoped lang="scss">
.student-management-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 56px);

  .breadcrumb {
    margin-bottom: 20px;
  }

  .search-card,
  .action-card,
  .table-card {
    margin-bottom: 20px;
    border-radius: 8px;
  }

  .search-form {
    :deep(.el-form-item) {
      margin-bottom: 0;
    }
  }

  .action-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .left-buttons {
      display: flex;
      gap: 10px;
    }

    .right-info {
      color: #909399;
      font-size: 14px;
    }
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
