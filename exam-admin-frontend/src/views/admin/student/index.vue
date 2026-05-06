<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>学生管理</span>
        </div>
      </template>
      
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="学号">
          <el-input v-model="queryParams.studentNo" placeholder="请输入学号" clearable />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="queryParams.realName" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      
      <!-- 操作按钮 -->
      <div class="toolbar">
        <el-button type="success" @click="handleAdd">
          <el-icon><Plus /></el-icon>新增学生
        </el-button>
      </div>
      
      <!-- 表格 -->
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="80">
          <template #default="{ row }">
            {{ row.gender === 1 ? '男' : '女' }}
          </template>
        </el-table-column>
        <el-table-column prop="className" label="班级" width="150" />
        <el-table-column prop="departmentName" label="院系" width="180" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>
    
    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <!-- 新增时显示默认密码提示 -->
      <el-alert v-if="!isEdit" title="提示" type="info" :closable="false" style="margin-bottom: 20px;">
        <template #default>
          <p style="margin: 0;">新增学生账号的<strong>默认密码为：123456</strong>，请通知学生首次登录后及时修改密码。</p>
        </template>
      </el-alert>
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="学号" prop="studentNo">
          <el-input v-model="formData.studentNo" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="formData.realName" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="formData.gender">
            <el-radio :label="1">男</el-radio>
            <el-radio :label="0">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" />
        </el-form-item>
        <el-form-item label="院系" prop="departmentId">
          <el-select v-model="formData.departmentId" placeholder="请选择院系" @change="handleDepartmentChange" clearable>
            <el-option v-for="dept in departmentList" :key="dept.id" :label="dept.deptName" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级" prop="classId">
          <el-select v-model="formData.classId" placeholder="请选择班级" clearable>
            <el-option v-for="cls in classList" :key="cls.id" :label="cls.className" :value="cls.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getStudentList, createStudent, updateStudent, deleteStudent, getDepartmentList, getClassList } from '@/api/admin/index'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref(null)
const departmentList = ref([])
const classList = ref([])

const queryParams = reactive({
  studentNo: '',
  realName: '',
  pageNum: 1,
  pageSize: 10
})

const formData = reactive({
  id: null,
  studentNo: '',
  realName: '',
  gender: 1,
  phone: '',
  email: '',
  departmentId: null,
  classId: null,
  status: 1
})

const rules = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getStudentList(queryParams)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error('获取学生列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  queryParams.pageNum = 1
  fetchData()
}

// 重置
const handleReset = () => {
  queryParams.studentNo = ''
  queryParams.realName = ''
  queryParams.pageNum = 1
  fetchData()
}

// 加载院系下拉数据
const loadDepartmentList = async () => {
  try {
    const deptRes = await getDepartmentList()
    departmentList.value = Array.isArray(deptRes.data) ? deptRes.data : (deptRes.data.records || [])
  } catch (error) {
    console.error('加载院系数据失败', error)
  }
}

// 根据院系加载班级下拉数据
const loadClassList = async (departmentId) => {
  try {
    const classRes = await getClassList({ departmentId })
    classList.value = Array.isArray(classRes.data) ? classRes.data : (classRes.data.records || [])
  } catch (error) {
    console.error('加载班级数据失败', error)
  }
}

// 院系变更处理
const handleDepartmentChange = (departmentId) => {
  formData.classId = null // 清空已选班级
  if (departmentId) {
    loadClassList(departmentId) // 根据院系加载班级
  } else {
    classList.value = [] // 如果未选择院系，清空班级列表
  }
}

// 加载下拉选项数据
const loadDropdownData = async () => {
  try {
    await loadDepartmentList()
    // 如果表单中已有院系信息，则加载对应的班级列表
    if (formData.departmentId) {
      await loadClassList(formData.departmentId)
    } else {
      classList.value = []
    }
  } catch (error) {
    console.error('加载下拉数据失败', error)
  }
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增学生'
  Object.assign(formData, {
    id: null,
    studentNo: '',
    realName: '',
    gender: 1,
    phone: '',
    email: '',
    departmentId: null,
    classId: null,
    status: 1
  })
  loadDropdownData()
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑学生'
  Object.assign(formData, row)
  loadDropdownData()
  dialogVisible.value = true
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该学生吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteStudent(row.id)
      ElMessage.success('删除成功')
      fetchData()
    } catch (error) {
      console.error('删除失败:', error)
    }
  }).catch(() => {})
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    if (isEdit.value) {
      await updateStudent(formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await createStudent(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    console.error('提交失败:', error)
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.page-container {
  padding: 20px;
  height: calc(100vh - 56px);
  box-sizing: border-box;
}

.el-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  
  :deep(.el-card__header) {
    flex-shrink: 0;
  }
  
  :deep(.el-card__body) {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    padding: 20px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
  flex-shrink: 0;
}

.toolbar {
  margin-bottom: 20px;
  flex-shrink: 0;
}

.el-table {
  flex: 1;
  overflow: auto;
  margin-bottom: 0;
}

:deep(.el-table__body-wrapper) {
  overflow-y: auto;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  flex-shrink: 0;
  background: #fff;
  padding: 16px 0;
}
</style>
