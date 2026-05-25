<template>
  <div class="exam-management-container">
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item :to="{ path: '/teacher/dashboard' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>考试管理</el-breadcrumb-item>
    </el-breadcrumb>

    <el-card class="action-card" shadow="never">
      <el-button type="primary" @click="handlePublishExam">
        <el-icon><Plus /></el-icon> 创建考试
      </el-button>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="examList">
        <el-table-column prop="examName" label="考试名称" min-width="200" />
        <el-table-column prop="className" label="班级" width="150" />
        <el-table-column prop="paperName" label="试卷名称" min-width="150" />
        <el-table-column label="开始时间" width="180">
          <template #default="{ row }">
            {{ row.startTime ? new Date(row.startTime).toLocaleString('zh-CN') : '' }}
          </template>
        </el-table-column>
        <el-table-column label="结束时间" width="180">
          <template #default="{ row }">
            {{ row.endTime ? new Date(row.endTime).toLocaleString('zh-CN') : '' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="参考人数" width="100">
          <template #default="{ row }">{{ row.participantCount || 0 }}/{{ row.totalCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleMonitor(row)" v-if="row.status===1">监控</el-button>
            <el-button type="warning" link @click="handleExtend(row)" v-if="row.status===1">延长</el-button>
            <el-button type="info" link @click="handlePreviewPaper(row)">预览</el-button>
            <el-button type="primary" link @click="handleView(row)">详情</el-button>
            <el-button type="success" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination v-model:current-page="pagination.pageNum" :total="total" layout="total, prev, pager, next" @current-change="fetchExams" />
      </div>
    </el-card>

    <!-- 创建考试 -->
    <el-dialog v-model="publishDialogVisible" :title="isEdit ? '编辑考试' : '创建考试'" width="700px">
      <el-form :model="examForm" label-width="100px">
        <el-form-item label="考试名称">
          <el-input v-model="examForm.examName" placeholder="请输入考试名称" />
        </el-form-item>
        <el-form-item label="选择试卷">
          <el-select v-model="examForm.paperId" placeholder="请选择试卷" style="width:100%">
            <el-option 
              v-for="paper in paperList" 
              :key="paper.id" 
              :label="paper.paperName" 
              :value="paper.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="参考班级">
          <el-select v-model="examForm.classId" placeholder="请选择班级" style="width:100%">
            <el-option 
              v-for="cls in classList" 
              :key="cls.classId" 
              :label="cls.className" 
              :value="cls.classId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker 
            v-model="examForm.startTime" 
            type="datetime" 
            placeholder="选择开始时间"
            style="width:100%" 
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker 
            v-model="examForm.endTime" 
            type="datetime" 
            placeholder="选择结束时间"
            style="width:100%" 
          />
        </el-form-item>
        <el-form-item label="启用乱序">
          <el-switch v-model="examForm.shuffleEnabled" active-text="启用" inactive-text="不启用" />
          <div style="font-size:12px; color:#909399; margin-top:5px;">启用后，每位学生看到的题目顺序和选项顺序将随机打乱</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishDialogVisible=false">取消</el-button>
        <el-button type="primary" @click="submitExam">{{ isEdit ? '确认修改' : '发布' }}</el-button>
      </template>
    </el-dialog>

    <!-- 实时监控 -->
    <el-dialog v-model="monitorDialogVisible" title="考试实时监控" width="900px" @close="stopAutoRefresh">
      <div class="monitor-stats">
        <el-row :gutter="20">
          <el-col :span="6"><el-tag size="large">应考: {{monitorData.totalCount}}</el-tag></el-col>
          <el-col :span="6"><el-tag type="success" size="large">已交卷: {{monitorData.submittedCount}}</el-tag></el-col>
          <el-col :span="6"><el-tag type="warning" size="large">考试中: {{monitorData.examiningCount}}</el-tag></el-col>
          <el-col :span="6"><el-tag type="danger" size="large">异常: {{monitorData.abnormalCount}}</el-tag></el-col>
        </el-row>
      </div>
      <el-alert 
        title="监控页面每10秒自动刷新数据" 
        type="info" 
        :closable="false" 
        style="margin-top: 10px" 
      />
      <el-table :data="monitorData.students" style="margin-top:20px">
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column label="状态" width="120">
          <template #default="{row}">
            <el-tag :type="row.status>=1?'success':'warning'">{{row.statusText}}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="150">
          <template #default="{row}">
            <el-progress :percentage="row.progress" />
          </template>
        </el-table-column>
        <el-table-column label="切屏次数" width="100">
          <template #default="{row}">
            <el-tag :type="row.switchCount > 3 ? 'danger' : 'info'">{{row.switchCount || 0}}次</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="异常情况" width="150">
          <template #default="{row}">
            <el-tag v-if="row.abnormal" type="danger" effect="dark">
              <el-icon><Warning /></el-icon>
              {{row.abnormalReason || '异常'}}
            </el-tag>
            <el-tag v-else type="success">正常</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{row}">
            <el-button 
              type="danger" 
              link 
              size="small" 
              @click="handleForceSubmit(row)"
              :disabled="row.status >= 1"
            >强制交卷</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 延长考试时间 -->
    <el-dialog v-model="extendDialogVisible" title="延长考试时间" width="600px">
      <el-form :model="extendForm" label-width="100px">
        <el-form-item label="选择班级">
          <el-select v-model="extendForm.classId" placeholder="请选择班级" style="width:100%" @change="handleClassChange">
            <el-option 
              v-for="cls in classList" 
              :key="cls.classId" 
              :label="cls.className" 
              :value="cls.classId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="选择学生">
          <el-select v-model="extendForm.studentId" placeholder="请选择学生" style="width:100%" :disabled="!extendForm.classId">
            <el-option 
              v-for="student in studentList" 
              :key="student.id" 
              :label="`${student.realName}(${student.studentNo})`" 
              :value="student.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="延长时间">
          <el-input-number v-model="extendForm.extendMinutes" :min="5" :max="120" /> 分钟
        </el-form-item>
        <el-form-item label="延长原因">
          <el-input v-model="extendForm.reason" type="textarea" :rows="3" placeholder="请输入延长原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="extendDialogVisible=false">取消</el-button>
        <el-button type="primary" @click="submitExtend">确认延长</el-button>
      </template>
    </el-dialog>

    <!-- 考试详情 -->
    <el-dialog v-model="detailDialogVisible" :title="'考试详情: ' + currentExam.examName" width="800px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="考试名称">{{ currentExam.examName }}</el-descriptions-item>
        <el-descriptions-item label="试卷名称">{{ currentExam.paperName }}</el-descriptions-item>
        <el-descriptions-item label="参考班级">{{ currentExam.className }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentExam.status)">{{ getStatusText(currentExam.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">
          {{ currentExam.startTime ? new Date(currentExam.startTime).toLocaleString('zh-CN') : '' }}
        </el-descriptions-item>
        <el-descriptions-item label="结束时间">
          {{ currentExam.endTime ? new Date(currentExam.endTime).toLocaleString('zh-CN') : '' }}
        </el-descriptions-item>
        <el-descriptions-item label="考试时长">{{ currentExam.duration || 120 }}分钟</el-descriptions-item>
        <el-descriptions-item label="总分">{{ currentExam.totalScore || 100 }}分</el-descriptions-item>
        <el-descriptions-item label="参考人数" :span="2">{{ currentExam.participantCount || 0 }} / {{ currentExam.totalCount || 0 }}</el-descriptions-item>
      </el-descriptions>
      
      <template #footer>
        <el-button @click="detailDialogVisible=false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 试卷预览 -->
    <el-dialog v-model="paperPreviewDialogVisible" :title="'试卷预览: ' + (previewPaper?.paperName || '')" width="900px" top="5vh">
      <div class="preview-header">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="科目">{{ previewPaper?.subject }}</el-descriptions-item>
          <el-descriptions-item label="总分">{{ previewPaper?.totalScore }}分</el-descriptions-item>
          <el-descriptions-item label="时长">{{ previewPaper?.duration }}分钟</el-descriptions-item>
          <el-descriptions-item label="难度">
            <el-tag :type="getDifficultyTagType(previewPaper?.difficulty)" size="small">
              {{ getDifficultyText(previewPaper?.difficulty) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="题目数量">{{ previewQuestions.length }}题</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTagType(previewPaper?.status)" size="small">
              {{ getStatusTextForPaper(previewPaper?.status) }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      
      <el-divider content-position="left">试卷内容</el-divider>
      
      <div v-loading="previewLoading" class="preview-content">
        <div v-if="previewQuestions.length === 0" class="empty-tip">
          <el-empty description="该试卷暂无题目" />
        </div>
        <div v-else class="question-list">
          <div v-for="(q, index) in previewQuestions" :key="q.id" class="question-item">
            <div class="question-header">
              <span class="question-number">{{ index + 1 }}.</span>
              <el-tag size="small" :type="getTypeTagType(q.type)">{{ getTypeText(q.type) }}</el-tag>
              <el-tag size="small" :type="getDifficultyTagType(q.difficulty)">{{ getDifficultyText(q.difficulty) }}</el-tag>
              <span class="question-score">（{{ q.score }}分）</span>
            </div>
            <div class="question-content">{{ q.content }}</div>
            
            <!-- 显示选项 -->
            <div v-if="q.type === 1 || q.type === 2" class="question-options">
              <div v-if="q.optionA" class="option-item">A. {{ q.optionA }}</div>
              <div v-if="q.optionB" class="option-item">B. {{ q.optionB }}</div>
              <div v-if="q.optionC" class="option-item">C. {{ q.optionC }}</div>
              <div v-if="q.optionD" class="option-item">D. {{ q.optionD }}</div>
            </div>
            
            <!-- 显示答案 -->
            <div v-if="q.answer" class="question-answer">
              <strong>答案：</strong>{{ q.answer }}
            </div>
            
            <!-- 显示解析 -->
            <div v-if="q.analysis" class="question-analysis">
              <strong>解析：</strong>{{ q.analysis }}
            </div>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="paperPreviewDialogVisible=false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Warning } from '@element-plus/icons-vue'
import { 
  getExamList, 
  publishExam, 
  updateExam,
  deleteExam,
  getExamMonitor,
  extendExamTime,
  forceSubmitExam,
  getMyClasses,
  getPaperList,
  getExamDetail,
  getStudentsByClassId,
  getPaperQuestions
} from '@/api/teacher/index'

const loading = ref(false)
const examList = ref([])
const total = ref(0)
const publishDialogVisible = ref(false)
const monitorDialogVisible = ref(false)
const extendDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const paperPreviewDialogVisible = ref(false)
const isEdit = ref(false)
const currentEditId = ref(null)
const pagination = reactive({ pageNum: 1, pageSize: 10 })

const examForm = reactive({ 
  examName: '', 
  paperId: null, 
  classId: null, 
  startTime: '', 
  endTime: '',
  shuffleEnabled: 1 // 默认启用乱序
})
const monitorData = reactive({ 
  totalCount: 0, 
  submittedCount: 0, 
  examiningCount: 0, 
  abnormalCount: 0, 
  students: [] 
})
const extendForm = reactive({ 
  examId: null,
  classId: null,
  studentId: null, 
  extendMinutes: 10, 
  reason: '' 
})
const currentExam = ref({})
const monitorInterval = ref(null) // 定时器
const previewLoading = ref(false)
const previewPaper = ref({})
const previewQuestions = ref([])

// 试卷列表和班级列表
const paperList = ref([])
const classList = ref([])
const studentList = ref([])

// 获取考试列表
const fetchExams = async () => {
  loading.value = true
  try {
    const res = await getExamList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    if (res.data) {
      // 后端直接返回数组
      if (Array.isArray(res.data)) {
        examList.value = res.data
        total.value = res.data.length
      } else {
        // 兼容旧的PageResult格式
        examList.value = res.data.records || []
        total.value = res.data.total || 0
      }
    }
  } catch (error) {
    console.error('获取考试列表失败:', error)
    ElMessage.error('获取考试列表失败')
  } finally {
    loading.value = false
  }
}

// 获取试卷列表（只获取已发布的）
const fetchPapers = async () => {
  try {
    const res = await getPaperList({
      pageNum: 1,
      pageSize: 100,
      status: 'published'
    })
    if (res.data && res.data.records) {
      paperList.value = res.data.records
    }
  } catch (error) {
    console.error('获取试卷列表失败:', error)
  }
}

// 获取班级列表
const fetchClasses = async () => {
  try {
    const res = await getMyClasses()
    if (res.data) {
      classList.value = res.data
    }
  } catch (error) {
    console.error('获取班级列表失败:', error)
  }
}

// 根据班级ID获取学生列表
const fetchStudents = async (classId) => {
  if (!classId) {
    studentList.value = []
    return
  }
  try {
    const res = await getStudentsByClassId(classId)
    if (res.data && res.data.records) {
      studentList.value = res.data.records
    }
  } catch (error) {
    console.error('获取学生列表失败:', error)
    ElMessage.error('获取学生列表失败')
  }
}

// 班级变化处理
const handleClassChange = (classId) => {
  extendForm.studentId = null // 清空已选学生
  fetchStudents(classId)
}

const handlePublishExam = () => { 
  resetForm() // 清空表单
  fetchPapers()
  fetchClasses()
  publishDialogVisible.value = true 
}

const submitExam = async () => {
  if (!examForm.examName) {
    ElMessage.warning('请输入考试名称')
    return
  }
  if (!examForm.paperId) {
    ElMessage.warning('请选择试卷')
    return
  }
  if (!examForm.classId) {
    ElMessage.warning('请选择班级')
    return
  }
  if (!examForm.startTime || !examForm.endTime) {
    ElMessage.warning('请选择考试时间')
    return
  }
  
  try {
    // 转换时间格式为 ISO 8601（后端Jackson可以解析）
    const formatTime = (time) => {
      if (!time) return ''
      const date = time instanceof Date ? time : new Date(time)
      // 转换为 "yyyy-MM-dd HH:mm:ss" 格式
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      const seconds = String(date.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
    }
    
    const submitData = {
      ...examForm,
      startTime: formatTime(examForm.startTime),
      endTime: formatTime(examForm.endTime)
    }
    
    if (isEdit.value) {
      await updateExam(currentEditId.value, submitData)
      ElMessage.success('修改成功')
    } else {
      await publishExam(submitData)
      ElMessage.success('考试创建成功')
    }
    publishDialogVisible.value = false
    resetForm()
    fetchExams()
  } catch (error) {
    console.error(isEdit.value ? '修改考试失败:' : '创建考试失败:', error)
    ElMessage.error(isEdit.value ? '修改失败' : '创建失败')
  }
}

const resetForm = () => {
  isEdit.value = false
  currentEditId.value = null
  examForm.examName = ''
  examForm.paperId = null
  examForm.classId = null
  examForm.startTime = ''
  examForm.endTime = ''
  examForm.shuffleEnabled = 1 // 重置为默认启用乱序
}

const handleMonitor = async (row) => {
  try {
    const res = await getExamMonitor(row.id)
    if (res.data) {
      monitorData.totalCount = res.data.totalCount || 0
      monitorData.submittedCount = res.data.submittedCount || 0
      monitorData.examiningCount = res.data.examiningCount || 0
      monitorData.abnormalCount = res.data.abnormalCount || 0
      monitorData.students = res.data.students || []
    }
    monitorDialogVisible.value = true
    
    // 启动自动刷新
    startAutoRefresh(row.id)
  } catch (error) {
    console.error('获取监控数据失败:', error)
    ElMessage.error('获取监控数据失败')
  }
}

const startAutoRefresh = (examId) => {
  stopAutoRefresh() // 清除之前的定时器
  monitorInterval.value = setInterval(async () => {
    try {
      const res = await getExamMonitor(examId)
      if (res.data) {
        monitorData.totalCount = res.data.totalCount || 0
        monitorData.submittedCount = res.data.submittedCount || 0
        monitorData.examiningCount = res.data.examiningCount || 0
        monitorData.abnormalCount = res.data.abnormalCount || 0
        monitorData.students = res.data.students || []
      }
    } catch (error) {
      console.error('自动刷新监控数据失败:', error)
    }
  }, 10000) // 每10秒刷新一次
}

const stopAutoRefresh = () => {
  if (monitorInterval.value) {
    clearInterval(monitorInterval.value)
    monitorInterval.value = null
  }
}

const handleExtend = (row) => {
  extendForm.examId = row.id
  extendForm.classId = row.classId || null // 自动选择该考试的班级
  extendForm.studentId = null
  extendForm.extendMinutes = 10
  extendForm.reason = ''
  studentList.value = [] // 清空学生列表
  
  // 加载班级列表
  fetchClasses()
  
  // 如果有班级，加载该班级的学生
  if (row.classId) {
    fetchStudents(row.classId)
  }
  
  extendDialogVisible.value = true
}

const submitExtend = async () => {
  if (!extendForm.classId) {
    ElMessage.warning('请选择班级')
    return
  }
  if (!extendForm.studentId) {
    ElMessage.warning('请选择学生')
    return
  }
  if (!extendForm.extendMinutes || extendForm.extendMinutes < 5) {
    ElMessage.warning('延长时间不能少于5分钟')
    return
  }
  
  try {
    await extendExamTime(extendForm)
    ElMessage.success('已延长考试时间')
    extendDialogVisible.value = false
  } catch (error) {
    console.error('延长考试时间失败:', error)
    ElMessage.error('延长考试时间失败')
  }
}

const handleView = async (row) => {
  try {
    const res = await getExamDetail(row.id)
    if (res.data) {
      currentExam.value = res.data
      detailDialogVisible.value = true
    }
  } catch (error) {
    console.error('获取考试详情失败:', error)
    ElMessage.error('获取考试详情失败')
  }
}

// 编辑考试
const handleEdit = async (row) => {
  try {
    // 先确保试卷和班级列表已加载
    await Promise.all([fetchPapers(), fetchClasses()])
    
    const res = await getExamDetail(row.id)
    if (res.data) {
      isEdit.value = true
      currentEditId.value = row.id
      examForm.examName = res.data.examName
      examForm.paperId = res.data.paperId
      examForm.classId = res.data.classId
      // 处理时间格式
      examForm.startTime = res.data.startTime ? new Date(res.data.startTime) : ''
      examForm.endTime = res.data.endTime ? new Date(res.data.endTime) : ''
      // 加载乱序设置，默认启用
      examForm.shuffleEnabled = res.data.shuffleEnabled !== undefined ? res.data.shuffleEnabled : 1
      publishDialogVisible.value = true
    }
  } catch (error) {
    console.error('获取考试详情失败:', error)
    ElMessage.error('获取考试详情失败')
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除此考试?', '提示', { type: 'warning' })
  try {
    await deleteExam(row.id)
    ElMessage.success('删除成功')
    fetchExams()
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('删除失败')
  }
}

const handleForceSubmit = async (row) => {
  await ElMessageBox.confirm(`确定强制 ${row.realName} 交卷?`, '提示', { type: 'warning' })
  try {
    await forceSubmitExam(extendForm.examId, row.studentId || row.id)
    ElMessage.success('已强制交卷')
    handleMonitor({ id: extendForm.examId })
  } catch (error) {
    console.error('强制交卷失败:', error)
    ElMessage.error('强制交卷失败')
  }
}

const handlePreviewPaper = async (row) => {
  // 获取试卷ID
  if (!row.paperId) {
    ElMessage.warning('该考试暂无关联试卷')
    return
  }
  
  previewPaper.value = { id: row.paperId, paperName: row.paperName }
  paperPreviewDialogVisible.value = true
  previewLoading.value = true
  previewQuestions.value = []
  
  try {
    const res = await getPaperQuestions(row.paperId)
    if (res.data) {
      previewQuestions.value = res.data
      // 如果试卷基本信息不完整，从第一题推断
      if (previewQuestions.value.length > 0) {
        // 计算总分
        const totalScore = previewQuestions.value.reduce((sum, q) => sum + (parseFloat(q.score) || 0), 0)
        previewPaper.value.totalScore = totalScore
        previewPaper.value.subject = row.subject || previewQuestions.value[0].subject || ''
        previewPaper.value.difficulty = 2
        previewPaper.value.duration = row.duration || 120
        previewPaper.value.status = 'published'
      }
    }
  } catch (error) {
    console.error('获取试卷题目失败:', error)
    ElMessage.error('获取试卷题目失败')
  } finally {
    previewLoading.value = false
  }
}

const getStatusTagType = (s) => ({ published: 'success', unpublished: 'info', ended: 'warning' }[s] || '')
const getStatusTextForPaper = (s) => ({ published: '已发布', unpublished: '未发布', ended: '已结束' }[s] || '')
const getDifficultyTagType = (d) => ({ 1: 'success', 2: 'warning', 3: 'danger' }[d] || '')
const getDifficultyText = (d) => ({ 1: '易', 2: '中', 3: '难' }[d] || '')
const getTypeTagType = (t) => ({ 1: 'primary', 2: 'warning', 3: 'success', 4: 'info', 5: 'danger' }[t] || '')
const getTypeText = (t) => ({ 1: '单选题', 2: '多选题', 3: '判断题', 4: '填空题', 5: '简答题' }[t] || '')

const getStatusType = (s) => ({ 0: 'info', 1: 'success', 2: 'warning' }[s] || '')
const getStatusText = (s) => ({ 0: '未开始', 1: '进行中', 2: '已结束' }[s] || '')

onMounted(() => {
  fetchExams()
})
</script>

<style scoped>
.exam-management-container { padding: 20px; background: #f5f7fa; min-height: calc(100vh - 56px); }
.breadcrumb { margin-bottom: 20px; }
.action-card, .table-card { margin-bottom: 20px; border-radius: 8px; }
.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
.monitor-stats { padding: 20px; background: #f9f9f9; border-radius: 8px; }
</style>
