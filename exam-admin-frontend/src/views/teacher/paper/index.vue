<template>
  <div class="paper-management-container">
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item :to="{ path: '/teacher/dashboard' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>试卷管理</el-breadcrumb-item>
    </el-breadcrumb>

    <el-card class="action-card" shadow="never">
      <div class="action-bar">
        <el-button type="primary" @click="handleManualCompose">
          <el-icon><EditPen /></el-icon> 手动组卷
        </el-button>
        <el-button type="success" @click="handleAutoCompose">
          <el-icon><MagicStick /></el-icon> 智能组卷
        </el-button>
      </div>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="paperList">
        <el-table-column prop="paperName" label="试卷名称" min-width="200" />
        <el-table-column prop="subject" label="科目" width="120" />
        <el-table-column prop="totalScore" label="总分" width="100" />
        <el-table-column prop="duration" label="时长" width="100">
          <template #default="{ row }">{{ row.duration }}分钟</template>
        </el-table-column>
        <el-table-column label="难度" width="100">
          <template #default="{ row }">
            <el-tag :type="getDifficultyTagType(row.difficulty)" size="small">{{ getDifficultyText(row.difficulty) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handlePreview(row)">预览</el-button>
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" link @click="handlePublish(row)">发布</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchPapers"
        />
      </div>
    </el-card>

    <!-- 手动组卷对话框 -->
    <el-dialog v-model="manualDialogVisible" title="手动组卷" width="900px" top="5vh">
      <el-form :model="paperForm" label-width="100px">
        <el-form-item label="试卷名称">
          <el-input v-model="paperForm.paperName" placeholder="请输入试卷名称" />
        </el-form-item>
        <el-form-item label="科目">
          <el-select 
            v-model="paperForm.subject" 
            placeholder="请选择科目" 
            style="width:100%"
            @change="handleSubjectChange"
          >
            <el-option 
              v-for="item in subjectList" 
              :key="item.id" 
              :label="item.name" 
              :value="item.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="时长">
          <el-input-number v-model="paperForm.duration" :min="30" :max="300" /> 分钟
        </el-form-item>
        <el-form-item label="试卷描述">
          <el-input v-model="paperForm.description" type="textarea" :rows="2" placeholder="请输入试卷描述（可选）" />
        </el-form-item>
        
        <el-divider>从题库选择题</el-divider>
        <div class="question-selector">
          <el-input 
            v-model="questionKeyword" 
            placeholder="搜索题目" 
            style="margin-bottom:15px" 
            @keyup.enter="searchQuestions"
          >
            <template #append>
              <el-button @click="searchQuestions">搜索</el-button>
            </template>
          </el-input>
          <el-table :data="availableQuestions" height="300" @selection-change="handleQuestionSelect">
            <el-table-column type="selection" width="55" />
            <el-table-column prop="content" label="题目" show-overflow-tooltip />
            <el-table-column prop="type" label="题型" width="100">
              <template #default="{row}">{{getTypeText(row.type)}}</template>
            </el-table-column>
            <el-table-column prop="difficulty" label="难度" width="80">
              <template #default="{row}">
                <el-tag :type="getDifficultyTagType(row.difficulty)" size="small">
                  {{getDifficultyText(row.difficulty)}}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="score" label="分值" width="80" />
          </el-table>
        </div>
        <div class="selected-summary">已选 {{ selectedQuestions.length }} 题，总分 {{ selectedTotalScore }} 分</div>
      </el-form>
      <template #footer>
        <el-button @click="manualDialogVisible=false">取消</el-button>
        <el-button type="primary" @click="savePaper">保存</el-button>
      </template>
    </el-dialog>

    <!-- 智能组卷对话框 -->
    <el-dialog v-model="autoDialogVisible" title="智能组卷" width="700px">
      <el-form :model="autoForm" label-width="120px">
        <el-form-item label="试卷名称">
          <el-input v-model="autoForm.paperName" placeholder="请输入试卷名称" />
        </el-form-item>
        <el-form-item label="科目">
          <el-select v-model="autoForm.subject" placeholder="请选择科目" style="width:100%">
            <el-option 
              v-for="item in subjectList" 
              :key="item.id" 
              :label="item.name" 
              :value="item.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="时长">
          <el-input-number v-model="autoForm.duration" :min="30" :max="300" /> 分钟
        </el-form-item>
        <el-form-item label="难度比例">
          <el-slider v-model="autoForm.easyRatio" :max="100" show-stops />
          <span>简单:{{autoForm.easyRatio}}% 中等:{{autoForm.mediumRatio}}% 困难:{{autoForm.hardRatio}}%</span>
        </el-form-item>
        <el-form-item label="题型配置">
          <div class="type-config">
            <el-input-number v-model="autoForm.singleCount" :min="0" style="width: 100px" /> 单选题 
            (<el-input-number v-model="autoForm.singleScore" :min="1" controls-position="right" style="width: 80px" /> 分/题)
          </div>
          <div class="type-config">
            <el-input-number v-model="autoForm.multiCount" :min="0" style="width: 100px" /> 多选题 
            (<el-input-number v-model="autoForm.multiScore" :min="1" controls-position="right" style="width: 80px" /> 分/题)
          </div>
          <div class="type-config">
            <el-input-number v-model="autoForm.judgeCount" :min="0" style="width: 100px" /> 判断题 
            (<el-input-number v-model="autoForm.judgeScore" :min="1" controls-position="right" style="width: 80px" /> 分/题)
          </div>
          <div class="type-config">
            <el-input-number v-model="autoForm.fillCount" :min="0" style="width: 100px" /> 填空题 
            (<el-input-number v-model="autoForm.fillScore" :min="1" controls-position="right" style="width: 80px" /> 分/题)
          </div>
          <div class="type-config">
            <el-input-number v-model="autoForm.essayCount" :min="0" style="width: 100px" /> 简答题 
            (<el-input-number v-model="autoForm.essayScore" :min="1" controls-position="right" style="width: 80px" /> 分/题)
          </div>
        </el-form-item>
        <el-form-item label="预计总分">
          <el-tag type="success" size="large">{{ calculateAutoTotal }} 分</el-tag>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="autoDialogVisible=false">取消</el-button>
        <el-button type="primary" @click="autoCompose">开始组卷</el-button>
      </template>
    </el-dialog>

    <!-- 试卷预览对话框 -->
    <el-dialog v-model="previewDialogVisible" :title="'预览试卷: ' + (previewPaper?.paperName || '')" width="900px" top="5vh">
      <div class="preview-header">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="科目">{{ previewPaper.subject }}</el-descriptions-item>
          <el-descriptions-item label="总分">{{ previewPaper.totalScore }}分</el-descriptions-item>
          <el-descriptions-item label="时长">{{ previewPaper.duration }}分钟</el-descriptions-item>
          <el-descriptions-item label="难度">
            <el-tag :type="getDifficultyTagType(previewPaper.difficulty)" size="small">
              {{ getDifficultyText(previewPaper.difficulty) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="题目数量">{{ previewQuestions.length }}题</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTagType(previewPaper.status)" size="small">
              {{ getStatusText(previewPaper.status) }}
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
            
            <!-- 显示选项（单选题、多选题） -->
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
        <el-button @click="previewDialogVisible=false">关闭</el-button>
        <el-button type="primary" @click="previewDialogVisible=false; handleEdit(previewPaper)">编辑试卷</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { EditPen, MagicStick } from '@element-plus/icons-vue'
import { 
  getPaperList, 
  createPaper, 
  updatePaper, 
  deletePaper,
  getSubjectList,
  getQuestionList
} from '@/api/teacher/index'
import request from '@/utils/request'

const loading = ref(false)
const paperList = ref([])
const total = ref(0)
const manualDialogVisible = ref(false)
const autoDialogVisible = ref(false)
const previewDialogVisible = ref(false)
const previewLoading = ref(false)
const previewPaper = ref({})
const previewQuestions = ref([])
const questionKeyword = ref('')
const availableQuestions = ref([])
const selectedQuestions = ref([])
const subjectList = ref([]) // 科目列表
const isEdit = ref(false) // 是否为编辑模式
const currentPaperId = ref(null) // 当前编辑的试卷ID

const pagination = reactive({ pageNum: 1, pageSize: 10 })

const paperForm = reactive({ 
  paperName: '', 
  subject: '', 
  duration: 120,
  description: ''
})

const autoForm = reactive({
  paperName: '', subject: '', duration: 120,
  easyRatio: 40, mediumRatio: 40, hardRatio: 20,
  singleCount: 20, singleScore: 2,
  multiCount: 10, multiScore: 3,
  judgeCount: 10, judgeScore: 1,
  fillCount: 5, fillScore: 5,
  essayCount: 2, essayScore: 10
})

// 组件挂载后确保分值正确初始化
onMounted(async () => {
  await nextTick()
  // 确保所有分值字段都是数字类型
  autoForm.singleScore = 2
  autoForm.multiScore = 3
  autoForm.judgeScore = 1
  autoForm.fillScore = 5
  autoForm.essayScore = 10
})

const selectedTotalScore = computed(() => 
  selectedQuestions.value.reduce((sum, q) => {
    const score = typeof q.score === 'number' ? q.score : parseFloat(q.score) || 0
    return sum + score
  }, 0)
)
const calculateAutoTotal = computed(() => 
  autoForm.singleCount * autoForm.singleScore + 
  autoForm.multiCount * autoForm.multiScore + 
  autoForm.judgeCount * autoForm.judgeScore +
  autoForm.fillCount * autoForm.fillScore +
  autoForm.essayCount * autoForm.essayScore
)

// 获取试卷列表
const fetchPapers = async () => {
  loading.value = true
  try {
    const res = await getPaperList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    if (res.data) {
      // 后端返回的是 records 字段
      paperList.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    console.error('获取试卷列表失败:', error)
    ElMessage.error('获取试卷列表失败')
  } finally {
    loading.value = false
  }
}

// 获取科目列表
const fetchSubjects = async () => {
  try {
    const res = await getSubjectList()
    if (res.data) {
      subjectList.value = res.data
    }
  } catch (error) {
    console.error('获取科目列表失败:', error)
  }
}

// 搜索可用题目
const searchQuestions = async () => {
  try {
    const res = await getQuestionList({
      keyword: questionKeyword.value,
      subject: paperForm.subject || undefined, // 如果选择了科目，按科目筛选
      pageNum: 1,
      pageSize: 50
    })
    if (res.data) {
      // 后端PageResult返回的是records字段，不是list
      availableQuestions.value = res.data.records || []
    }
  } catch (error) {
    console.error('搜索题目失败:', error)
  }
}

// 科目改变时重新搜索题目
const handleSubjectChange = () => {
  selectedQuestions.value = [] // 清空已选题目
  searchQuestions()
}

const handleManualCompose = () => { 
  isEdit.value = false
  currentPaperId.value = null
  resetPaperForm()
  manualDialogVisible.value = true 
  searchQuestions()
}

const handleAutoCompose = () => { 
  autoDialogVisible.value = true 
}

const handlePreview = async (row) => {
  previewPaper.value = row
  previewDialogVisible.value = true
  previewLoading.value = true
  
  try {
    // 解析question_config获取题目ID列表
    if (row.questionConfig) {
      const config = JSON.parse(row.questionConfig)
      if (config.questions && config.questions.length > 0) {
        // 获取所有题目ID
        const questionIds = config.questions.map(q => q.questionId)
        
        // 批量获取题目详情
        const res = await getQuestionList({
          pageNum: 1,
          pageSize: 100
        })
        
        if (res.data && res.data.records) {
          // 筛选出试卷中的题目
          previewQuestions.value = res.data.records.filter(q => questionIds.includes(q.id))
          
          // 按照试卷中的顺序排列
          previewQuestions.value.sort((a, b) => {
            const indexA = questionIds.indexOf(a.id)
            const indexB = questionIds.indexOf(b.id)
            return indexA - indexB
          })
        }
      } else {
        previewQuestions.value = []
      }
    } else {
      previewQuestions.value = []
    }
  } catch (error) {
    console.error('获取试卷题目失败:', error)
    ElMessage.error('获取试卷题目失败')
    previewQuestions.value = []
  } finally {
    previewLoading.value = false
  }
}

const handleEdit = async (row) => {
  isEdit.value = true
  currentPaperId.value = row.id
  
  // 填充表单
  paperForm.paperName = row.paperName
  paperForm.subject = row.subject
  paperForm.duration = row.duration
  paperForm.description = row.description || ''
  
  manualDialogVisible.value = true
  searchQuestions()
}

const handlePublish = async (row) => {
  await ElMessageBox.confirm('确定发布此试卷?', '提示', { type: 'warning' })
  try {
    // 更新试卷状态为已发布
    await updatePaper(row.id, { ...row, status: 'published' })
    ElMessage.success('发布成功')
    fetchPapers()
  } catch (error) {
    console.error('发布失败:', error)
    ElMessage.error('发布失败')
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除此试卷?', '提示', { type: 'warning' })
  try {
    await deletePaper(row.id)
    ElMessage.success('删除成功')
    fetchPapers()
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('删除失败')
  }
}

const handleQuestionSelect = (selection) => { 
  selectedQuestions.value = selection 
}

// 重置表单
const resetPaperForm = () => {
  paperForm.paperName = ''
  paperForm.subject = ''
  paperForm.duration = 120
  paperForm.description = ''
  selectedQuestions.value = []
}

const savePaper = async () => {
  if (!paperForm.paperName) {
    ElMessage.warning('请输入试卷名称')
    return
  }
  if (!paperForm.subject) {
    ElMessage.warning('请选择科目')
    return
  }
  if (selectedQuestions.value.length === 0) {
    ElMessage.warning('请至少选择一道题目')
    return
  }
  
  try {
    // 计算平均难度（四舍五入）
    const avgDifficulty = Math.round(
      selectedQuestions.value.reduce((sum, q) => sum + (q.difficulty || 2), 0) / selectedQuestions.value.length
    )
    
    const paperData = {
      ...paperForm,
      totalScore: selectedTotalScore.value,
      difficulty: avgDifficulty || 2, // 默认中等难度
      // 将选中的题目ID保存为JSON
      questionConfig: JSON.stringify({
        questions: selectedQuestions.value.map(q => ({
          questionId: q.id,
          score: q.score
        }))
      }),
      status: 'unpublished'
    }
    
    if (isEdit.value && currentPaperId.value) {
      await updatePaper(currentPaperId.value, paperData)
      ElMessage.success('试卷更新成功')
    } else {
      await createPaper(paperData)
      ElMessage.success('试卷创建成功')
    }
    
    manualDialogVisible.value = false
    fetchPapers()
  } catch (error) {
    console.error('保存试卷失败:', error)
    ElMessage.error('保存试卷失败')
  }
}

const autoCompose = async () => {
  if (!autoForm.paperName) {
    ElMessage.warning('请输入试卷名称')
    return
  }
  if (!autoForm.subject) {
    ElMessage.warning('请选择科目')
    return
  }
  
  try {
    const paperData = {
      paperName: autoForm.paperName,
      subject: autoForm.subject,
      duration: autoForm.duration,
      typeConfig: {
        singleCount: autoForm.singleCount,
        singleScore: autoForm.singleScore,
        multiCount: autoForm.multiCount,
        multiScore: autoForm.multiScore,
        judgeCount: autoForm.judgeCount,
        judgeScore: autoForm.judgeScore,
        fillCount: autoForm.fillCount,
        fillScore: autoForm.fillScore,
        essayCount: autoForm.essayCount,
        essayScore: autoForm.essayScore
      }
    }
    
    // 调用后端智能组卷接口
    await request.post('/teacher/papers/auto-compose', paperData)
    
    ElMessage.success('智能组卷成功')
    autoDialogVisible.value = false
    fetchPapers()
  } catch (error) {
    console.error('智能组卷失败:', error)
    ElMessage.error('智能组卷失败')
  }
}

const getDifficultyTagType = (d) => ({1:'success',2:'warning',3:'danger'}[d]||'')
const getDifficultyText = (d) => ({1:'简单',2:'中等',3:'困难'}[d]||'')
const getStatusTagType = (s) => ({unpublished:'info',published:'success',ended:'warning'}[s]||'')
const getStatusText = (s) => ({unpublished:'未发布',published:'已发布',ended:'已结束'}[s]||'')
const getTypeText = (t) => ({1:'单选题',2:'多选题',3:'判断题',4:'填空题',5:'简答题'}[t]||'')
const getTypeTagType = (t) => ({1:'',2:'warning',3:'success',4:'info',5:'danger'}[t]||'')

onMounted(() => {
  fetchPapers()
  fetchSubjects()
})
</script>

<style scoped>
.paper-management-container { padding: 20px; background: #f5f7fa; min-height: calc(100vh - 56px); }
.breadcrumb { margin-bottom: 20px; }
.action-card, .table-card { margin-bottom: 20px; border-radius: 8px; }
.action-bar { display: flex; gap: 10px; }
.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
.question-selector { margin: 15px 0; }
.selected-summary { margin-top: 15px; font-weight: bold; color: #409eff; }
.type-config { margin-bottom: 10px; }

/* 预览样式 */
.preview-header { margin-bottom: 20px; }
.preview-content { max-height: 60vh; overflow-y: auto; }
.question-list { padding: 0 10px; }
.question-item {
  margin-bottom: 25px;
  padding: 15px;
  background: #f9f9f9;
  border-radius: 8px;
  border-left: 4px solid #409eff;
}
.question-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.question-number {
  font-weight: bold;
  font-size: 16px;
  color: #409eff;
}
.question-score {
  color: #909399;
  font-size: 14px;
}
.question-content {
  font-size: 15px;
  line-height: 1.8;
  color: #303133;
  margin-bottom: 10px;
}
.question-options {
  margin: 10px 0;
  padding-left: 20px;
}
.option-item {
  line-height: 2;
  color: #606266;
}
.question-answer {
  margin-top: 10px;
  padding: 8px 12px;
  background: #e1f3d8;
  border-radius: 4px;
  color: #67c23a;
}
.question-answer strong {
  color: #67c23a;
}
.question-analysis {
  margin-top: 10px;
  padding: 8px 12px;
  background: #fff3e0;
  border-radius: 4px;
  color: #e6a23c;
}
.question-analysis strong {
  color: #e6a23c;
}
.empty-tip {
  text-align: center;
  padding: 40px 0;
}
</style>
