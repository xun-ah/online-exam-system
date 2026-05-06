<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>科目管理</span>
        </div>
      </template>
      
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="科目名称">
          <el-input v-model="queryParams.name" placeholder="请输入科目名称" clearable />
        </el-form-item>
        <el-form-item label="所属院系">
          <el-select v-model="queryParams.departmentId" placeholder="请选择院系" clearable style="width: 200px">
            <el-option v-for="dept in departmentList" :key="dept.id" :label="dept.name" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      
      <!-- 操作按钮 -->
      <div class="toolbar">
        <el-button type="success" @click="handleAdd">
          <el-icon><Plus /></el-icon>新增科目
        </el-button>
      </div>
      
      <!-- 表格 -->
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="科目名称" />
        <el-table-column prop="code" label="科目代码" width="150" />
        <el-table-column prop="departmentName" label="所属院系" width="180" />
        <el-table-column prop="credits" label="学分" width="100" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
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
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="科目名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入科目名称" />
        </el-form-item>
        <el-form-item label="科目代码" prop="code">
          <el-input v-model="formData.code" placeholder="请输入科目代码，如：CS101" />
        </el-form-item>
        <el-form-item label="所属院系" prop="departmentId">
          <el-select v-model="formData.departmentId" placeholder="请选择院系" clearable style="width: 100%">
            <el-option v-for="dept in departmentList" :key="dept.id" :label="dept.name" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="学分" prop="credits">
          <el-input-number v-model="formData.credits" :min="0" :max="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入科目描述" />
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
import request from '@/utils/request'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref(null)
const departmentList = ref([])

const queryParams = reactive({
  name: '',
  departmentId: null,
  pageNum: 1,
  pageSize: 10
})

const formData = reactive({
  id: null,
  name: '',
  code: '',
  departmentId: null,
  credits: 3,
  description: '',
  status: 1
})

const rules = {
  name: [{ required: true, message: '请输入科目名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入科目代码', trigger: 'blur' }],
  departmentId: [{ required: true, message: '请选择所属院系', trigger: 'change' }]
}

// 加载院系列表
const loadDepartmentList = async () => {
  try {
    const res = await request.get('/admin/departments')
    departmentList.value = res.data.records || res.data || []
  } catch (error) {
    console.error('加载院系列表失败:', error)
  }
}

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      name: queryParams.name,
      departmentId: queryParams.departmentId
    }
    const res = await request.get('/subject/list/admin', { params })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error('获取科目列表失败:', error)
    ElMessage.error('获取科目列表失败')
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
  queryParams.name = ''
  queryParams.departmentId = null
  queryParams.pageNum = 1
  fetchData()
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增科目'
  Object.assign(formData, {
    id: null,
    name: '',
    code: '',
    departmentId: null,
    credits: 3,
    description: '',
    status: 1
  })
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑科目'
  Object.assign(formData, row)
  dialogVisible.value = true
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该科目吗？删除后相关题目和试卷将受到影响。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/subject/${row.id}`)
      ElMessage.success('删除成功')
      fetchData()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }).catch(() => {})
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    if (isEdit.value) {
      await request.put(`/subject/${formData.id}`, formData)
      ElMessage.success('更新成功')
    } else {
      await request.post('/subject', formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    console.error('提交失败:', error)
    const message = error.response?.data?.message || error.message || '操作失败'
    ElMessage.error(message)
  }
}

onMounted(() => {
  loadDepartmentList()
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
