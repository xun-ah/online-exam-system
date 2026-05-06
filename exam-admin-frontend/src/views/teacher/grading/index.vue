<template>
  <div class="grading-management-container">
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item :to="{ path: '/teacher/dashboard' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>阅卷与成绩</el-breadcrumb-item>
    </el-breadcrumb>

    <el-tabs v-model="activeTab" type="card">
      <!-- 待阅卷 -->
      <el-tab-pane label="待阅卷" name="pending">
        <el-card class="tab-card" shadow="never">
          <el-table :data="pendingList" v-loading="loading">
            <el-table-column prop="examName" label="考试名称" min-width="200" />
            <el-table-column prop="className" label="班级" width="150" />
            <el-table-column label="待阅人数" width="120">
              <template #default="{row}"><el-tag type="danger">{{row.pendingCount}}</el-tag></template>
            </el-table-column>
            <el-table-column prop="submitTime" label="提交时间" width="180" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{row}"><el-button type="primary" @click="handleGrade(row)">开始阅卷</el-button></template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 已阅卷 -->
      <el-tab-pane label="已阅卷" name="graded">
        <el-card class="tab-card" shadow="never">
          <el-form :inline="true" class="search-form">
            <el-form-item label="考试">
              <el-select v-model="searchForm.examId" placeholder="全部" style="width:180px" clearable>
                <el-option 
                  v-for="exam in examOptions" 
                  :key="exam.id" 
                  :label="exam.examName" 
                  :value="exam.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="班级">
              <el-select v-model="searchForm.classId" placeholder="全部" style="width:150px" clearable>
                <el-option 
                  v-for="cls in classOptions" 
                  :key="cls.id" 
                  :label="cls.className" 
                  :value="cls.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item><el-button type="primary" @click="handleSearch">搜索</el-button></el-form-item>
          </el-form>
          <el-table :data="gradedList" style="margin-top:15px" v-loading="loading">
            <el-table-column prop="studentNo" label="学号" width="120" />
            <el-table-column prop="realName" label="姓名" width="100" />
            <el-table-column label="客观题得分" width="120"><template #default="{row}">{{row.objectiveScore}}/{{row.objectiveTotal}}</template></el-table-column>
            <el-table-column label="主观题得分" width="120"><template #default="{row}">{{row.subjectiveScore}}/{{row.subjectiveTotal}}</template></el-table-column>
            <el-table-column prop="totalScore" label="总分" width="100"><template #default="{row}"><span style="color:#67c23a;font-weight:bold">{{row.totalScore}}</span></template></el-table-column>
            <el-table-column label="操作" width="120"><template #default="{row}"><el-button type="primary" link @click="handleViewDetail(row)">详情</el-button></template></el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 成绩统计 -->
      <el-tab-pane label="成绩统计" name="statistics">
        <el-card class="tab-card" shadow="never">
          <el-form :inline="true">
            <el-form-item label="考试">
              <el-select v-model="statsForm.examId" style="width:180px" placeholder="请选择考试">
                <el-option 
                  v-for="exam in examOptions" 
                  :key="exam.id" 
                  :label="exam.examName" 
                  :value="exam.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="fetchStatistics">查询</el-button>
              <el-button type="success" @click="handleExport">导出Excel</el-button>
            </el-form-item>
          </el-form>
          
          <el-row :gutter="20" style="margin-top:20px">
            <el-col :span="6">
              <el-card shadow="never">
                <div style="text-align:center">
                  <div style="font-size:12px;color:#909399">最高分</div>
                  <div style="font-size:28px;font-weight:bold;color:#67c23a">{{statistics.maxScore}}<small>分</small></div>
                </div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="never">
                <div style="text-align:center">
                  <div style="font-size:12px;color:#909399">最低分</div>
                  <div style="font-size:28px;font-weight:bold;color:#f56c6c">{{statistics.minScore}}<small>分</small></div>
                </div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="never">
                <div style="text-align:center">
                  <div style="font-size:12px;color:#909399">平均分</div>
                  <div style="font-size:28px;font-weight:bold;color:#409eff">{{statistics.avgScore}}<small>分</small></div>
                </div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="never">
                <div style="text-align:center">
                  <div style="font-size:12px;color:#909399">及格率</div>
                  <div style="font-size:28px;font-weight:bold;color:#e6a23c">{{statistics.passRate}}<small>%</small></div>
                </div>
              </el-card>
            </el-col>
          </el-row>

          <div style="margin-top:30px">
            <h4>分数段分布</h4>
            <el-progress :percentage="statistics.excellent" color="#67c23a" /> 优秀(90-100): {{statistics.excellent}}%
            <el-progress :percentage="statistics.good" color="#409eff" /> 良好(80-89): {{statistics.good}}%
            <el-progress :percentage="statistics.medium" color="#e6a23c" /> 中等(70-79): {{statistics.medium}}%
            <el-progress :percentage="statistics.pass" color="#909399" /> 及格(60-69): {{statistics.pass}}%
            <el-progress :percentage="statistics.fail" color="#f56c6c" /> 不及格(<60): {{statistics.fail}}%
          </div>

          <div style="margin-top:30px">
            <h4>错题分析 - 错误率TOP5</h4>
            <el-table :data="errorQuestions" style="margin-top:15px">
              <el-table-column prop="content" label="题目内容" show-overflow-tooltip />
              <el-table-column prop="errorRate" label="错误率" width="150"><template #default="{row}"><el-tag type="danger">{{row.errorRate}}%</el-tag></template></el-table-column>
              <el-table-column prop="errorCount" label="错误人数" width="120" />
            </el-table>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 阅卷对话框 -->
    <el-dialog v-model="gradeDialogVisible" title="在线阅卷" width="900px">
      <div class="grading-content">
        <el-alert type="info" :closable="false">客观题已自动批改，请对主观题进行评分</el-alert>
        <div class="question-list">
          <div v-for="q in subjectiveQuestions" :key="q.id" class="question-item">
            <div class="question-header">
              <span class="question-number">{{q.number}}.</span>
              <span class="question-type">{{q.type}}</span>
              <span class="question-score">满分: {{q.fullScore}}分</span>
            </div>
            <div class="question-content">{{q.content}}</div>
            <div class="student-answer">
              <h5>学生答案:</h5>
              <p>{{q.studentAnswer}}</p>
            </div>
            <div class="grading-section">
              <h5>评分:</h5>
              <el-input-number v-model="q.score" :min="0" :max="q.fullScore" size="small" />
              <span style="margin-left:10px">/ {{q.fullScore}}分</span>
            </div>
          </div>
        </div>
      </div>
      <template #footer><el-button @click="gradeDialogVisible=false">取消</el-button><el-button type="primary" @click="submitGrade">提交评分</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getPendingGrading, 
  getGradedRecords, 
  getGradingDetail, 
  submitGrading,
  getScoreStatistics,
  exportExamScores,
  getExamList,
  getMyClasses
} from '@/api/teacher/index'

const activeTab = ref('pending')
const loading = ref(false)
const pendingList = ref([])
const gradedList = ref([])
const gradeDialogVisible = ref(false)
const currentRecord = reactive({
  recordId: null,
  studentName: '',
  studentNo: ''
})
const subjectiveQuestions = ref([])
const searchForm = reactive({ examId: null, classId: null })
const statsForm = reactive({ examId: null })

const statistics = reactive({ 
  maxScore: 0, 
  minScore: 0, 
  avgScore: '0.0', 
  passRate: '0.0', 
  excellent: 0, 
  good: 0, 
  medium: 0, 
  pass: 0, 
  fail: 0 
})

const errorQuestions = ref([])

// 考试和班级选项
const examOptions = ref([])
const classOptions = ref([])

// 获取待阅卷列表
const fetchPendingGrading = async () => {
  loading.value = true
  try {
    const res = await getPendingGrading()
    if (res.data) {
      pendingList.value = res.data
    }
  } catch (error) {
    console.error('获取待阅卷列表失败:', error)
    ElMessage.error('获取待阅卷列表失败')
  } finally {
    loading.value = false
  }
}

// 获取已阅卷列表
const fetchGradedRecords = async () => {
  loading.value = true
  try {
    const params = {}
    if (searchForm.examId) params.examId = searchForm.examId
    if (searchForm.classId) params.classId = searchForm.classId
    
    const res = await getGradedRecords(params)
    if (res.data) {
      gradedList.value = res.data
    }
  } catch (error) {
    console.error('获取已阅卷列表失败:', error)
    ElMessage.error('获取已阅卷列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索已阅卷
const handleSearch = () => {
  fetchGradedRecords()
}

// 开始阅卷
const handleGrade = async (row) => {
  try {
    const res = await getGradingDetail(row.examId)
    if (res.data) {
      currentRecord.recordId = res.data.recordId
      currentRecord.studentName = res.data.studentName
      currentRecord.studentNo = res.data.studentNo
      subjectiveQuestions.value = res.data.subjectiveQuestions || []
      gradeDialogVisible.value = true
    }
  } catch (error) {
    console.error('获取阅卷详情失败:', error)
    ElMessage.error('获取阅卷详情失败')
  }
}

// 提交阅卷
const submitGrade = async () => {
  try {
    const data = {
      recordId: currentRecord.recordId,
      subjectiveScore: subjectiveQuestions.value.reduce((sum, q) => sum + (q.score || 0), 0)
    }
    await submitGrading(data)
    ElMessage.success('评分提交成功')
    gradeDialogVisible.value = false
    fetchPendingGrading()
    fetchGradedRecords()
  } catch (error) {
    console.error('提交评分失败:', error)
    ElMessage.error('提交评分失败')
  }
}

// 查看详情
const handleViewDetail = (row) => {
  ElMessage.info('查看成绩详情')
}

// 获取考试列表
const fetchExams = async () => {
  try {
    const res = await getExamList({ pageNum: 1, pageSize: 100 })
    if (res.data && res.data.records) {
      examOptions.value = res.data.records.map(exam => ({
        id: exam.id,
        examName: exam.examName
      }))
    }
  } catch (error) {
    console.error('获取考试列表失败:', error)
  }
}

// 获取班级列表
const fetchClasses = async () => {
  try {
    const res = await getMyClasses()
    if (res.data) {
      classOptions.value = res.data.map(cls => ({
        id: cls.classId,
        className: cls.className
      }))
    }
  } catch (error) {
    console.error('获取班级列表失败:', error)
  }
}

// 导出成绩
const handleExport = async () => {
  try {
    await exportExamScores({ examId: statsForm.examId })
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}

// 获取成绩统计
const fetchStatistics = async () => {
  if (!statsForm.examId) {
    ElMessage.warning('请选择考试')
    return
  }
  
  try {
    const res = await getScoreStatistics(statsForm.examId)
    if (res.data) {
      Object.assign(statistics, res.data)
    }
  } catch (error) {
    console.error('获取成绩统计失败:', error)
    ElMessage.error('获取成绩统计失败')
  }
}

onMounted(() => {
  fetchPendingGrading()
  fetchExams()
  fetchClasses()
})
</script>

<style scoped>
.grading-management-container { padding: 20px; background: #f5f7fa; min-height: calc(100vh - 56px); }
.breadcrumb { margin-bottom: 20px; }
.tab-card { border-radius: 8px; }
.search-form { margin-bottom: 10px; }
.grading-content .question-list { margin-top: 20px; }
.question-item { padding: 20px; margin-bottom: 20px; border: 1px solid #ebeef5; border-radius: 8px; }
.question-header { display: flex; align-items: center; gap: 10px; margin-bottom: 15px; }
.question-number { font-weight: bold; font-size: 16px; }
.question-type { color: #909399; }
.question-score { margin-left: auto; color: #409eff; }
.question-content, .student-answer { margin-bottom: 15px; line-height: 1.6; }
.student-answer h5 { margin: 0 0 10px 0; }
.grading-section { display: flex; align-items: center; gap: 10px; padding: 15px; background: #f9f9f9; border-radius: 6px; }
.grading-section h5 { margin: 0; }
</style>
