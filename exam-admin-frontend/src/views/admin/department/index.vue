<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>院系管理</span>
        </div>
      </template>
      
      <!-- 操作按钮 -->
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>新增院系
        </el-button>
      </div>
      
      <!-- 表格 -->
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="code" label="院系代码" width="150" />
        <el-table-column prop="name" label="院系名称" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="院系代码" prop="code">
          <el-input v-model="formData.code" placeholder="请输入院系代码" />
        </el-form-item>
        <el-form-item label="院系名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入院系名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDepartmentList, createDepartment, updateDepartment, deleteDepartment } from '@/api/admin'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增院系')
const formRef = ref(null)
const isEdit = ref(false)

const pagination = ref({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const formData = ref({
  id: null,
  code: '',
  name: '',
  description: ''
})

const rules = {
  code: [
    { required: true, message: '请输入院系代码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (isEdit.value && formData.value.code === value) {
          callback()
          return
        }
        const exists = tableData.value.some(item => item.code === value)
        if (exists) {
          callback(new Error('院系代码已存在'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  name: [
    { required: true, message: '请输入院系名称', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (isEdit.value && formData.value.name === value) {
          callback()
          return
        }
        const exists = tableData.value.some(item => item.name === value)
        if (exists) {
          callback(new Error('院系名称已存在'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.value.pageNum,
      pageSize: pagination.value.pageSize
    }
    const res = await getDepartmentList(params)
    
    // 处理分页数据
    if (res.data && res.data.records) {
      tableData.value = res.data.records
      pagination.value.total = res.data.total || 0
    } else {
      tableData.value = res.data || []
      pagination.value.total = tableData.value.length
    }
  } catch (error) {
    console.error('加载院系列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增院系'
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑院系'
  formData.value = { ...row }
  dialogVisible.value = true
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该院系吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteDepartment(row.id)
      ElMessage.success('删除成功')
      loadData()
    } catch (error) {
      console.error('删除失败:', error)
    }
  }).catch(() => {})
}

// 提交
const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (isEdit.value) {
          await updateDepartment(formData.value.id, formData.value)
          ElMessage.success('更新成功')
        } else {
          await createDepartment(formData.value)
          ElMessage.success('新增成功')
        }
        dialogVisible.value = false
        loadData()
      } catch (error) {
        console.error('提交失败:', error)
        const message = error.response?.data?.message || error.message || '操作失败'
        ElMessage.error(message)
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 对话框关闭
const handleDialogClose = () => {
  formRef.value?.resetFields()
  formData.value = {
    id: null,
    code: '',
    name: '',
    description: ''
  }
}

onMounted(() => {
  loadData()
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
