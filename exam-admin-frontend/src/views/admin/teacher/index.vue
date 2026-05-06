<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>教师管理</span>
        </div>
      </template>
      
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="工号">
          <el-input v-model="queryParams.teacherNo" placeholder="请输入工号" clearable />
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
          <el-icon><Plus /></el-icon>新增教师
        </el-button>
      </div>
      
      <!-- 表格 -->
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="teacherNo" label="工号" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="80">
          <template #default="{ row }">
            {{ row.gender === 1 ? '男' : '女' }}
          </template>
        </el-table-column>
        <el-table-column prop="departmentName" label="所属院系" width="150" />
        <el-table-column label="任教科目" width="250">
          <template #default="{ row }">
            <el-tag v-for="(subject, index) in row.subjects" :key="index" size="small" style="margin: 2px">
              {{ subject }}
            </el-tag>
            <span v-if="!row.subjects || row.subjects.length === 0" style="color: #999">未分配</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" size="small" @click="handleAssignClass(row)">分配班级</el-button>
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
          <p style="margin: 0;">新增教师账号的<strong>默认密码为：123456</strong>，请通知教师首次登录后及时修改密码。</p>
        </template>
      </el-alert>
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="工号" prop="teacherNo">
          <el-input v-model="formData.teacherNo" :disabled="isEdit" />
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
          <el-select v-model="formData.departmentId" placeholder="请选择院系" clearable>
            <el-option v-for="dept in departmentList" :key="dept.id" :label="dept.name || dept.deptName" :value="dept.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 分配班级对话框 -->
    <el-dialog v-model="classDialogVisible" title="分配班级" width="700px">
      <el-form :model="classFormData" label-width="100px">
        <el-form-item label="教师姓名">
          <el-input v-model="currentTeacher.realName" disabled />
        </el-form-item>
        <el-form-item label="选择班级">
          <el-checkbox-group v-model="classFormData.classIds">
            <el-checkbox v-for="cls in classList" :key="cls.id" :label="cls.id">
              {{ cls.className }} ({{ cls.grade }}级)
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="任教科目">
          <el-select v-model="classFormData.subject" placeholder="请选择任教科目" clearable style="width: 100%">
            <el-option v-for="subj in subjectList" :key="subj.id" :label="subj.name" :value="subj.name" />
          </el-select>
          <el-button type="primary" link size="small" @click="handleAddSubject" style="margin-top: 8px">
            <el-icon><Plus /></el-icon>新增科目
          </el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="classDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitClassAssignment">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTeacherList, createTeacher, updateTeacher, deleteTeacher, getDepartmentList, getClassList } from '@/api/admin/index'
import request from '@/utils/request'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref(null)
const departmentList = ref([])
const classDialogVisible = ref(false)
const currentTeacher = ref({})
const classList = ref([])
const subjectList = ref([])

const queryParams = reactive({
  teacherNo: '',
  realName: '',
  pageNum: 1,
  pageSize: 10
})

const formData = reactive({
  id: null,
  teacherNo: '',
  realName: '',
  gender: 1,
  phone: '',
  email: '',
  departmentId: null,
  status: 1
})

const classFormData = reactive({
  classIds: [],
  subject: ''
})

const rules = {
  teacherNo: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  departmentId: [{ required: true, message: '请选择院系', trigger: 'change' }]
}

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getTeacherList(queryParams)
    const teachers = res.data.records || []
    
    // 为每位教师加载任教科目
    for (const teacher of teachers) {
      try {
        const subjectRes = await request.get(`/admin/teachers/${teacher.id}/classes`)
        const teacherClasses = subjectRes.data || []
        // 提取不重复的科目名称
        const subjects = [...new Set(teacherClasses.map(tc => tc.subject).filter(s => s))]
        teacher.subjects = subjects
      } catch (error) {
        console.error(`获取教师${teacher.realName}的科目失败:`, error)
        teacher.subjects = []
      }
    }
    
    tableData.value = teachers
    total.value = res.data.total || 0
  } catch (error) {
    console.error('获取教师列表失败:', error)
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
  queryParams.teacherNo = ''
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

// 新增
const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增教师'
  Object.assign(formData, {
    id: null,
    teacherNo: '',
    realName: '',
    gender: 1,
    phone: '',
    email: '',
    departmentId: null,
    status: 1
  })
  loadDepartmentList()
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑教师'
  Object.assign(formData, row)
  loadDepartmentList()
  dialogVisible.value = true
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该教师吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteTeacher(row.id)
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
      await updateTeacher(formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await createTeacher(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    console.error('提交失败:', error)
    // 显示后端返回的错误信息
    const message = error.response?.data?.message || error.message || '操作失败'
    ElMessage.error(message)
  }
}

// 分配班级
const handleAssignClass = async (row) => {
  console.log('Teacher row data:', row)
  currentTeacher.value = row
  classFormData.classIds = []
  classFormData.subject = ''
  
  // 加载班级列表(只加载教师所属院系的班级)
  try {
    // 获取所有班级（不限制分页，确保所有院系的班级都能被过滤到）
    const res = await getClassList({ pageSize: 100 })
    // 处理分页数据，提取records数组
    const allClasses = res.data?.records || res.data || []
    
    // 过滤出教师所属院系的班级
    classList.value = allClasses.filter(cls => cls.departmentId === row.departmentId)
    
    console.log('Teacher ID:', row.id)
    console.log('Teacher Department ID:', row.departmentId)
    console.log('Filtered classes:', classList.value)
    
    // 获取该教师已分配的班级
    const assignedRes = await request.get(`/admin/teachers/${row.id}/classes`)
    const assignedClasses = assignedRes.data || []
    classFormData.classIds = assignedClasses.map(tc => tc.classId)
    
    // 获取该院系的科目列表
    console.log('正在获取科目列表, 院系ID:', row.departmentId)
    const subjectRes = await request.get(`/subject/list/admin?departmentId=${row.departmentId}&pageSize=100`)
    subjectList.value = subjectRes.data?.records || subjectRes.data || []
    console.log('获取到的科目列表:', subjectList.value.map(s => ({id: s.id, name: s.name, status: s.status})))
    
    classDialogVisible.value = true
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  }
}

// 新增科目
const handleAddSubject = () => {
  // 跳转到科目管理页面
  window.location.href = '/admin/subject'
}

// 提交班级分配
const handleSubmitClassAssignment = async () => {
  // 校验是否选择了班级
  if (!classFormData.classIds || classFormData.classIds.length === 0) {
    ElMessage.warning('请至少选择一个班级')
    return
  }
  
  // 校验是否选择了科目
  if (!classFormData.subject) {
    ElMessage.warning('请选择任教科目')
    return
  }
  
  console.log('=== 开始提交班级分配 ===')
  console.log('教师ID:', currentTeacher.value.id)
  console.log('选择的班级IDs:', classFormData.classIds)
  console.log('选择的科目名称:', classFormData.subject)
  console.log('科目列表:', subjectList.value.map(s => ({id: s.id, name: s.name})))
  
  try {
    await request.post(`/admin/teachers/${currentTeacher.value.id}/assign-classes`, {
      classIds: classFormData.classIds,
      subject: classFormData.subject
    })
    console.log('分配成功!')
    ElMessage.success('分配成功')
    classDialogVisible.value = false
    // 刷新数据
    fetchData()
  } catch (error) {
    console.error('分配失败:', error)
    const errorMsg = error.response?.data?.message || error.message || '分配失败'
    ElMessage.error(errorMsg)
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
