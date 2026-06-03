 <template>
  <div class="question-bank-container">
    <!-- 面包屑导航 -->
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item :to="{ path: '/teacher/dashboard' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>题库管理</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card" shadow="never">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
              <el-icon :size="30"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalQuestions }}</div>
              <div class="stat-label">题目总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="never">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
              <el-icon :size="30"><Plus /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.monthQuestions }}</div>
              <div class="stat-label">本月新增</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="never">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
              <el-icon :size="30"><Collection /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.subjectCount }}</div>
              <div class="stat-label">覆盖科目</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="never">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);">
              <el-icon :size="30"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.usageCount }}</div>
              <div class="stat-label">被引用次数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索和操作区 -->
    <el-card class="search-card" shadow="hover">
      <div class="filter-container">
        <!-- 筛选条件区 -->
        <div class="filter-section">
          <div class="section-title">
            <el-icon><Filter /></el-icon>
            <span>筛选条件</span>
          </div>
          <div class="filter-content">
            <div class="filter-item">
              <label>科目</label>
              <el-select v-model="searchForm.subject" placeholder="全部科目" clearable>
                <el-option 
                  v-for="item in subjectList" 
                  :key="item.id" 
                  :label="item.name" 
                  :value="item.name"
                />
              </el-select>
            </div>
            <div class="filter-item">
              <label>题型</label>
              <el-select v-model="searchForm.type" placeholder="全部题型" clearable>
                <el-option label="单选题" :value="1" />
                <el-option label="多选题" :value="2" />
                <el-option label="判断题" :value="3" />
                <el-option label="填空题" :value="4" />
                <el-option label="简答题" :value="5" />
                <el-option label="编程题" :value="6" />
              </el-select>
            </div>
            <div class="filter-item">
              <label>难度</label>
              <el-select v-model="searchForm.difficulty" placeholder="全部难度" clearable>
                <el-option label="简单" :value="1" />
                <el-option label="中等" :value="2" />
                <el-option label="困难" :value="3" />
              </el-select>
            </div>
            <div class="filter-item keyword-item">
              <label>关键词</label>
              <el-input v-model="searchForm.keyword" placeholder="搜索题目内容" clearable>
                <template #suffix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </div>
            <div class="filter-item button-item">
              <el-button @click="handleSearch" type="primary">
                <el-icon><Search /></el-icon>
                搜索
              </el-button>
              <el-button @click="handleReset">
                <el-icon><RefreshLeft /></el-icon>
                重置
              </el-button>
            </div>
          </div>
        </div>

        <!-- 操作按钮区 -->
        <div class="action-section">
          <div class="section-title">
            <el-icon><Operation /></el-icon>
            <span>操作</span>
          </div>
          <div class="action-content">
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              新增题目
            </el-button>
            <el-button type="success" @click="handleImportDialog">
              <el-icon><Upload /></el-icon>
              批量导入
            </el-button>
            <el-button type="warning" @click="handleExport">
              <el-icon><Download /></el-icon>
              批量导出
            </el-button>
            <el-button 
              type="danger" 
              @click="handleBatchDelete"
              :disabled="selectedQuestions.length === 0"
            >
              <el-icon><Delete /></el-icon>
              批量删除 ({{ selectedQuestions.length }})
            </el-button>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 题目列表 -->
    <el-card class="table-card" shadow="never">
      <el-table 
        v-loading="loading" 
        :data="questionList" 
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column label="序号" width="80">
          <template #default="{ $index }">
            {{ (pagination.pageNum - 1) * pagination.pageSize + $index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="content" label="题目内容" min-width="300" show-overflow-tooltip />
        <el-table-column label="题型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)" size="small">
              {{ getTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="100">
          <template #default="{ row }">
            <el-tag :type="getDifficultyTagType(row.difficulty)" size="small">
              {{ getDifficultyText(row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="subject" label="科目" width="120" />
        <el-table-column prop="score" label="分值" width="80" />
        <el-table-column prop="usageCount" label="引用次数" width="100" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">预览</el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" link size="small" @click="handleCopy(row)">复制</el-button>
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

    <!-- 新增/编辑题目对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="900px"
      @close="handleDialogClose"
    >
      <el-form :model="questionForm" :rules="rules" ref="questionFormRef" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="题目类型" prop="type">
              <el-radio-group v-model="questionForm.type" @change="handleTypeChange">
                <el-radio :label="1">单选题</el-radio>
                <el-radio :label="2">多选题</el-radio>
                <el-radio :label="3">判断题</el-radio>
                <el-radio :label="4">填空题</el-radio>
                <el-radio :label="5">简答题</el-radio>
                <el-radio :label="6">编程题</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="难度" prop="difficulty">
              <el-radio-group v-model="questionForm.difficulty">
                <el-radio :label="1">简单</el-radio>
                <el-radio :label="2">中等</el-radio>
                <el-radio :label="3">困难</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="科目" prop="subject">
          <el-select v-model="questionForm.subject" placeholder="请选择科目" style="width: 100%">
            <el-option 
              v-for="item in subjectList" 
              :key="item.id" 
              :label="item.name" 
              :value="item.name" 
            />
          </el-select>
        </el-form-item>

        <el-form-item label="题目内容" prop="content">
          <el-input v-model="questionForm.content" type="textarea" :rows="4" placeholder="请输入题目内容" />
        </el-form-item>

        <!-- 选择题选项 -->
        <template v-if="questionForm.type <= 2">
          <el-form-item label="选项A" prop="optionA">
            <el-input v-model="questionForm.optionA" placeholder="请输入选项A内容" />
          </el-form-item>
          <el-form-item label="选项B" prop="optionB">
            <el-input v-model="questionForm.optionB" placeholder="请输入选项B内容" />
          </el-form-item>
          <el-form-item label="选项C" prop="optionC">
            <el-input v-model="questionForm.optionC" placeholder="请输入选项C内容" />
          </el-form-item>
          <el-form-item label="选项D" prop="optionD">
            <el-input v-model="questionForm.optionD" placeholder="请输入选项D内容" />
          </el-form-item>
        </template>

        <el-form-item label="正确答案" prop="answer" v-if="questionForm.type <= 3">
          <el-input v-model="questionForm.answer" :placeholder="getAnswerPlaceholder(questionForm.type)" />
          <div class="form-tip" v-if="questionForm.type === 2">多选题答案格式：AB、AC、ABC等</div>
          <div class="form-tip" v-if="questionForm.type === 3">判断题答案格式：true 或 false</div>
          <div class="form-tip" v-if="questionForm.type === 4">多个答案用英文逗号分隔，如：答案1,答案2,答案3</div>
        </el-form-item>
        
        <el-form-item label="参考答案" v-if="questionForm.type === 5">
          <el-input v-model="questionForm.answer" type="textarea" :rows="3" placeholder="请输入参考答案（仅作为阅卷参考，非必填）" />
          <div class="form-tip">简答题无需标准答案，此内容仅供教师阅卷时参考</div>
        </el-form-item>

        <!-- 编程题专用字段 -->
        <template v-if="questionForm.type === 6">
          <el-form-item label="编程语言" prop="language">
            <el-select v-model="questionForm.language" placeholder="请选择编程语言" style="width: 100%">
              <el-option label="Java" value="Java" />
              <el-option label="Python" value="Python" />
              <el-option label="C++" value="C++" />
              <el-option label="C" value="C" />
              <el-option label="JavaScript" value="JavaScript" />
            </el-select>
          </el-form-item>

          <el-form-item label="代码模板" prop="codeTemplate">
            <el-input v-model="questionForm.codeTemplate" type="textarea" :rows="8" placeholder="请输入代码模板（支持使用 ``` 包裹代码块）" />
          </el-form-item>

          <el-form-item label="测试用例" prop="testCases">
            <el-input v-model="questionForm.testCases" type="textarea" :rows="6" placeholder='请输入测试用例（JSON格式），例如：[{&quot;input&quot;:&quot;1 2&quot;,&quot;output&quot;:&quot;3&quot;}]' />
          </el-form-item>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="时间限制" prop="timeLimit">
                <el-input-number v-model="questionForm.timeLimit" :min="100" :max="10000" :step="100" style="width: 100%" />
                <span class="unit-text">毫秒</span>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="内存限制" prop="memoryLimit">
                <el-input-number v-model="questionForm.memoryLimit" :min="32" :max="1024" :step="32" style="width: 100%" />
                <span class="unit-text">MB</span>
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <el-form-item label="答案解析">
          <el-input v-model="questionForm.analysis" type="textarea" :rows="3" placeholder="请输入题目解析（选填）" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="分值" prop="score">
              <el-input-number v-model="questionForm.score" :min="1" :max="100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="知识点标签">
              <el-select v-model="questionForm.tags" multiple placeholder="选择知识点标签" style="width: 100%">
                <el-option label="基础语法" value="基础语法" />
                <el-option label="面向对象" value="面向对象" />
                <el-option label="集合框架" value="集合框架" />
                <el-option label="多线程" value="多线程" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入对话框 -->
    <el-dialog v-model="importDialogVisible" title="批量导入题目" width="600px">
      <div class="import-content">
        <el-alert title="导入说明" type="info" :closable="false" style="margin-bottom: 20px">
          <template #default>
            <p>1. 请按照模板格式填写题目信息</p>
            <p>2. 支持Excel(.xlsx)和Word(.docx)格式</p>
            <p>3. 单次最多导入500道题目</p>
            <p>4. 导入前请先选择科目，所有题目将归属于该科目</p>
          </template>
        </el-alert>
        
        <el-form label-width="80px">
          <el-form-item label="所属科目">
            <el-select v-model="importSubject" placeholder="请选择科目" style="width: 100%">
              <el-option 
                v-for="item in subjectList" 
                :key="item.id" 
                :label="item.name" 
                :value="item.name"
              />
            </el-select>
          </el-form-item>
        </el-form>
        
        <el-upload
          class="upload-demo"
          drag
          action="#"
          :auto-upload="false"
          :on-change="handleFileChange"
          accept=".xlsx,.docx"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            将文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              只能上传 xlsx/docx 文件
            </div>
          </template>
        </el-upload>

        <div class="template-download">
          <el-button type="primary" link @click="downloadTemplate">
            <el-icon><Download /></el-icon>
            下载导入模板
          </el-button>
        </div>
      </div>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleImportSubmit" :disabled="!importFile || !importSubject">开始导入</el-button>
      </template>
    </el-dialog>

    <!-- 批量导出对话框 -->
    <el-dialog v-model="exportDialogVisible" title="批量导出题目" width="600px">
      <div class="export-content">
        <el-alert title="导出说明" type="info" :closable="false" style="margin-bottom: 20px">
          <template #default>
            <p>1. 导出的题目将包含在Excel文件中</p>
            <p>2. 可根据筛选条件导出指定题目</p>
            <p>3. 不选择筛选条件将导出所有题目</p>
          </template>
        </el-alert>
        
        <el-form label-width="80px">
          <el-form-item label="所属科目">
            <el-select v-model="exportSubject" placeholder="请选择科目（可选）" clearable style="width: 100%">
              <el-option 
                v-for="item in subjectList" 
                :key="item.id" 
                :label="item.name" 
                :value="item.name"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="题型">
            <el-select v-model="exportType" placeholder="请选择题型（可选）" clearable style="width: 100%">
              <el-option label="单选题" :value="1" />
              <el-option label="多选题" :value="2" />
              <el-option label="判断题" :value="3" />
              <el-option label="填空题" :value="4" />
              <el-option label="简答题" :value="5" />
              <el-option label="编程题" :value="6" />
            </el-select>
          </el-form-item>
          <el-form-item label="难度">
            <el-select v-model="exportDifficulty" placeholder="请选择难度（可选）" clearable style="width: 100%">
              <el-option label="简单" :value="1" />
              <el-option label="中等" :value="2" />
              <el-option label="困难" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="关键词">
            <el-input v-model="exportKeyword" placeholder="请输入题目内容关键词（可选）" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="exportDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleExportSubmit">开始导出</el-button>
      </template>
    </el-dialog>

    <!-- 题目预览对话框 -->
    <el-dialog v-model="previewDialogVisible" title="题目预览" width="700px">
      <div class="question-preview">
        <div class="preview-header">
          <el-tag :type="getTypeTagType(currentQuestion.type)">{{ getTypeText(currentQuestion.type) }}</el-tag>
          <el-tag :type="getDifficultyTagType(currentQuestion.difficulty)">{{ getDifficultyText(currentQuestion.difficulty) }}</el-tag>
          <span class="preview-score">{{ currentQuestion.score }}分</span>
        </div>
        <div class="preview-content">
          <h4>题目：</h4>
          <p>{{ currentQuestion.content }}</p>
        </div>
        <div class="preview-options" v-if="currentQuestion.type <= 2">
          <h4>选项：</h4>
          <template v-if="currentQuestion.options">
            <div v-for="(opt, index) in parsePreviewOptions(currentQuestion.options)" :key="index" class="option-item">
              {{ String.fromCharCode(65 + index) }}. {{ opt }}
            </div>
          </template>
          <template v-else>
            <p>A. {{ currentQuestion.optionA || '' }}</p>
            <p>B. {{ currentQuestion.optionB || '' }}</p>
            <p>C. {{ currentQuestion.optionC || '' }}</p>
            <p>D. {{ currentQuestion.optionD || '' }}</p>
          </template>
        </div>
        <div class="preview-answer">
          <h4>正确答案：</h4>
          <p style="color: #67c23a; font-weight: bold;">{{ currentQuestion.answer }}</p>
        </div>
        <div class="preview-analysis" v-if="currentQuestion.analysis">
          <h4>答案解析：</h4>
          <p>{{ currentQuestion.analysis }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Document, Plus, Collection, TrendCharts, Search, Upload, Download, UploadFilled, Delete,
  Filter, Operation, RefreshLeft
} from '@element-plus/icons-vue'
import { getQuestionList, createQuestion, updateQuestion, deleteQuestion, batchDeleteQuestions, getSubjectList, exportQuestions } from '@/api/teacher/index'
import request from '@/utils/request'

const loading = ref(false)
const subjectList = ref([]) // 科目列表
const questionList = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('新增题目')
const questionFormRef = ref(null)
const importDialogVisible = ref(false)
const exportDialogVisible = ref(false)
const previewDialogVisible = ref(false)
const importFile = ref(null)
const importSubject = ref('') // 导入时选择的科目

// 导出相关变量
const exportSubject = ref('')
const exportType = ref(null)
const exportDifficulty = ref(null)
const exportKeyword = ref('')

const selectedQuestions = ref([]) // 选中的题目
const currentQuestion = ref({})

const stats = reactive({
  totalQuestions: 0,
  monthQuestions: 0,
  subjectCount: 0,
  usageCount: 0
})

const searchForm = reactive({
  subject: '',
  type: null,
  difficulty: null,
  keyword: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10
})

const questionForm = reactive({
  id: null,
  type: 1,
  difficulty: 1,
  subject: '',
  content: '',
  optionA: '',
  optionB: '',
  optionC: '',
  optionD: '',
  answer: '',
  analysis: '',
  score: 5,
  tags: [],
  language: 'Java',
  codeTemplate: '',
  testCases: '',
  timeLimit: 1000,
  memoryLimit: 256
})

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

const rules = {
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  subject: [{ required: true, message: '请选择科目', trigger: 'change' }],
  content: [{ required: true, message: '请输入题目内容', trigger: 'blur' }],
  answer: [
    { 
      required: true, 
      message: '请输入正确答案', 
      trigger: 'blur',
      validator: (rule, value, callback) => {
        // 简答题不需要验证答案
        if (questionForm.type === 5) {
          callback()
        } else if (!value) {
          callback(new Error('请输入正确答案'))
        } else {
          callback()
        }
      }
    }
  ],
  score: [{ required: true, message: '请输入分值', trigger: 'blur' }]
}

// 解析预览选项
const parsePreviewOptions = (optionsData) => {
  if (!optionsData) return []
  
  // 如果已经是数组
  if (Array.isArray(optionsData)) {
    return optionsData
  }
  
  try {
    // 尝试解析JSON
    const parsed = JSON.parse(optionsData)
    if (Array.isArray(parsed)) {
      return parsed
    }
    // 如果是对象格式 {"A": "选项1", "B": "选项2"}，转换为数组
    if (typeof parsed === 'object') {
      return Object.values(parsed)
    }
    return []
  } catch (e) {
    // 如果不是JSON，尝试用|分隔
    if (typeof optionsData === 'string') {
      return optionsData.split('|').filter(o => o.trim())
    }
    return []
  }
}

// 获取统计数据
const fetchStats = async () => {
  try {
    const res = await request.get('/teacher/questions/stats')
    if (res.data) {
      stats.totalQuestions = res.data.totalQuestions || 0
      stats.monthQuestions = res.data.monthQuestions || 0
      stats.subjectCount = res.data.subjectCount || 0
      stats.usageCount = res.data.usageCount || 0
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 搜索
const fetchQuestions = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      type: searchForm.type,
      difficulty: searchForm.difficulty,
      keyword: searchForm.keyword,
      subject: searchForm.subject
    }
    
    const res = await getQuestionList(params)
    if (res.data) {
      questionList.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    console.error('获取题目列表失败:', error)
    ElMessage.error('获取题目列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1
  fetchQuestions()
}

// 重置
const handleReset = () => {
  Object.assign(searchForm, { subject: '', type: null, difficulty: null, keyword: '' })
  handleSearch()
}

// 新增
const handleAdd = () => {
  dialogTitle.value = '新增题目'
  resetForm()
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  dialogTitle.value = '编辑题目'
  Object.assign(questionForm, row)
  dialogVisible.value = true
}

// 预览
const handleView = (row) => {
  currentQuestion.value = row
  previewDialogVisible.value = true
}

// 复制
const handleCopy = (row) => {
  dialogTitle.value = '复制题目'
  Object.assign(questionForm, row)
  questionForm.id = null
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这道题目吗？删除后无法恢复。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteQuestion(row.id)
    ElMessage.success('删除成功')
    fetchQuestions()
    fetchStats()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 选中变化
const handleSelectionChange = (selection) => {
  selectedQuestions.value = selection
}

// 批量删除
const handleBatchDelete = async () => {
  if (selectedQuestions.value.length === 0) {
    ElMessage.warning('请先选择要删除的题目')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedQuestions.value.length} 道题目吗？删除后无法恢复。`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 获取所有选中题目的ID
    const ids = selectedQuestions.value.map(q => q.id)
    
    // 调用批量删除API
    await batchDeleteQuestions(ids)
    
    ElMessage.success('批量删除成功')
    selectedQuestions.value = []
    fetchQuestions()
    fetchStats()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
      ElMessage.error('批量删除失败')
    }
  }
}

// 题型变化
const handleTypeChange = (type) => {
  if (type === 3) {
    questionForm.optionA = '正确'
    questionForm.optionB = '错误'
    questionForm.optionC = ''
    questionForm.optionD = ''
  }
}

// 导入对话框
const handleImportDialog = () => {
  importFile.value = null
  importSubject.value = '' // 重置科目选择
  importDialogVisible.value = true
}

// 文件选择
const handleFileChange = (file) => {
  importFile.value = file
}

// 下载模板
const downloadTemplate = () => {
  ElMessage.info('下载模板功能开发中')
  // TODO: 下载Excel/Word模板
}

// 提交导入
const handleImportSubmit = async () => {
  if (!importFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  
  if (!importSubject.value) {
    ElMessage.warning('请选择科目')
    return
  }
  
  try {
    // 调用后端导入API
    const res = await batchImportQuestions(importFile.value.raw, importSubject.value)
    
    if (res.code === 200) {
      ElMessage.success(res.data.message || '导入成功')
      importDialogVisible.value = false
      importFile.value = null
      importSubject.value = ''
      // 刷新列表和统计
      fetchQuestions()
      fetchStats()
    } else {
      ElMessage.error(res.message || '导入失败')
    }
  } catch (error) {
    console.error('导入失败:', error)
    ElMessage.error('导入失败: ' + (error.message || '未知错误'))
  }
}

// 导出
const handleExport = async () => {
  // 打开导出对话框
  exportDialogVisible.value = true
}

// 导出提交
const handleExportSubmit = async () => {
  try {
    // 构建查询参数
    const params = {
      pageNum: 1,
      pageSize: 1,
      type: exportType.value,
      difficulty: exportDifficulty.value,
      keyword: exportKeyword.value,
      subject: exportSubject.value
    }
    
    // 先查询符合条件的题目数量
    const countRes = await getQuestionList(params)
    const totalCount = countRes.data?.total || 0
    
    // 如果没有符合条件的题目，友好提示用户
    if (totalCount === 0) {
      let conditionDesc = '所选筛选条件'
      const conditions = []
      if (exportSubject.value) conditions.push(`科目：${exportSubject.value}`)
      if (exportType.value) conditions.push(`题型：${getTypeText(exportType.value)}`)
      if (exportDifficulty.value) conditions.push(`难度：${getDifficultyText(exportDifficulty.value)}`)
      if (exportKeyword.value) conditions.push(`关键词：${exportKeyword.value}`)
      
      if (conditions.length > 0) {
        conditionDesc = conditions.join('、')
      }
      
      ElMessage.warning(`${conditionDesc} 没有匹配的题目，请调整筛选条件后再导出`)
      return
    }
    
    // 构建导出参数
    const exportParams = {
      type: exportType.value,
      difficulty: exportDifficulty.value,
      keyword: exportKeyword.value,
      subject: exportSubject.value
    }
    
    // 调用导出API
    const res = await exportQuestions(exportParams)
    
    // 检查是否是错误响应（blob可能是JSON错误信息）
    if (res.type === 'application/json') {
      const text = await res.text()
      const errorData = JSON.parse(text)
      ElMessage.error(errorData.message || '导出失败')
      return
    }
    
    // 处理blob响应，创建下载链接
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '题目导出_' + new Date().getTime() + '.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('导出成功')
    exportDialogVisible.value = false
    
    // 重置导出表单
    exportSubject.value = ''
    exportType.value = null
    exportDifficulty.value = null
    exportKeyword.value = ''
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}

// 分页
const handleSizeChange = (size) => {
  pagination.pageSize = size
  fetchQuestions()
}

const handlePageChange = (page) => {
  pagination.pageNum = page
  fetchQuestions()
}

// 提交表单
const handleSubmit = async () => {
  try {
    await questionFormRef.value.validate()
    
    // 准备提交数据
    const submitData = {
      ...questionForm,
      score: questionForm.score,
      // 将选项拼接为JSON
      options: questionForm.type <= 2 ? JSON.stringify({
        A: questionForm.optionA,
        B: questionForm.optionB,
        C: questionForm.optionC,
        D: questionForm.optionD
      }) : null
    }
    
    // 编程题需要清理不需要的字段
    if (questionForm.type === 6) {
      submitData.optionA = null
      submitData.optionB = null
      submitData.optionC = null
      submitData.optionD = null
    }
    
    if (questionForm.id) {
      // 编辑
      await updateQuestion(questionForm.id, submitData)
      ElMessage.success('更新成功')
    } else {
      // 新增
      await createQuestion(submitData)
      ElMessage.success('新增成功')
    }
    
    dialogVisible.value = false
    fetchQuestions()
    fetchStats()
  } catch (error) {
    if (error !== false) {
      console.error('保存失败:', error)
      ElMessage.error('保存失败')
    }
  }
}

// 关闭对话框
const handleDialogClose = () => {
  questionFormRef.value?.resetFields()
}

// 重置表单
const resetForm = () => {
  Object.assign(questionForm, {
    id: null,
    type: 1,
    difficulty: 1,
    subject: '',
    content: '',
    optionA: '',
    optionB: '',
    optionC: '',
    optionD: '',
    answer: '',
    analysis: '',
    score: 5,
    tags: [],
    language: 'Java',
    codeTemplate: '',
    testCases: '',
    timeLimit: 1000,
    memoryLimit: 256
  })
}

// 辅助函数
const getTypeTagType = (type) => {
  const types = { 1: '', 2: 'success', 3: 'warning', 4: 'danger', 5: 'info', 6: 'primary' }
  return types[type] || ''
}

const getTypeText = (type) => {
  const texts = { 1: '单选题', 2: '多选题', 3: '判断题', 4: '填空题', 5: '简答题', 6: '编程题' }
  return texts[type] || '未知'
}

const getDifficultyTagType = (difficulty) => {
  const types = { 1: 'success', 2: 'warning', 3: 'danger' }
  return types[difficulty] || ''
}

const getDifficultyText = (difficulty) => {
  const texts = { 1: '简单', 2: '中等', 3: '困难' }
  return texts[difficulty] || '未知'
}

// 获取答案输入框的占位符
const getAnswerPlaceholder = (type) => {
  const placeholders = {
    1: '请输入正确答案（A/B/C/D）',
    2: '请输入正确答案（如：AB、AC、ABC等）',
    3: '请输入正确答案（true/false）',
    4: '请输入正确答案（多个答案用英文逗号分隔）'
  }
  return placeholders[type] || '请输入正确答案'
}

onMounted(() => {
  fetchSubjects() // 加载科目列表
  fetchStats()
  fetchQuestions()
})
</script>

<style scoped lang="scss">
.question-bank-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 56px);

  .breadcrumb {
    margin-bottom: 20px;
  }

  .stats-row {
    margin-bottom: 20px;

    .stat-card {
      border-radius: 8px;

      .stat-content {
        display: flex;
        align-items: center;
        gap: 15px;

        .stat-icon {
          width: 60px;
          height: 60px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: #fff;
        }

        .stat-info {
          flex: 1;

          .stat-value {
            font-size: 28px;
            font-weight: bold;
            color: #303133;
            line-height: 1.2;
          }

          .stat-label {
            font-size: 14px;
            color: #909399;
            margin-top: 4px;
          }
        }
      }
    }
  }

  .search-card {
    margin-bottom: 20px;
    border-radius: 12px;
    border: none;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
    transition: all 0.3s ease;

    &:hover {
      box-shadow: 0 4px 20px 0 rgba(0, 0, 0, 0.08);
    }

    :deep(.el-card__body) {
      padding: 20px;
    }

    .filter-container {
      display: flex;
      gap: 24px;

      .filter-section {
        flex: 1;
        background: #fff;
        border-radius: 10px;
        padding: 16px 20px;
        position: relative;
        overflow: hidden;
        border: 1px solid #e8e9eb;

        .section-title {
          display: flex;
          align-items: center;
          gap: 8px;
          color: #303133;
          font-size: 14px;
          font-weight: 600;
          margin-bottom: 16px;
          position: relative;
          z-index: 1;

          .el-icon {
            font-size: 16px;
            color: #409eff;
          }
        }

        .filter-content {
          display: grid;
          grid-template-columns: repeat(4, 1fr);
          gap: 16px;
          position: relative;
          z-index: 1;

          .filter-item {
            display: flex;
            flex-direction: column;
            gap: 6px;

            label {
              font-size: 12px;
              color: #606266;
              font-weight: 500;
            }

            &.keyword-item {
              grid-column: span 2;
            }

            &.button-item {
              flex-direction: row;
              align-items: flex-end;
              gap: 10px;
            }

            :deep(.el-select),
            :deep(.el-input) {
              .el-input__wrapper {
                background: #f5f7fa;
                box-shadow: none;
                border: 1px solid #dcdfe6;
                border-radius: 8px;
                transition: all 0.3s ease;

                &:hover {
                  background: #fff;
                  border-color: #c0c4cc;
                }

                &.is-focus {
                  background: #fff;
                  border-color: #409eff;
                  box-shadow: 0 0 0 1px #409eff inset;
                }

                .el-input__inner {
                  color: #303133;
                  
                  &::placeholder {
                    color: #909399;
                  }
                }
              }
            }
          }
        }
      }

      .action-section {
        min-width: 200px;
        background: #f8f9fa;
        border-radius: 10px;
        padding: 16px 20px;
        border: 1px solid #e8e9eb;

        .section-title {
          display: flex;
          align-items: center;
          gap: 8px;
          color: #606266;
          font-size: 14px;
          font-weight: 600;
          margin-bottom: 16px;

          .el-icon {
            font-size: 16px;
            color: #409eff;
          }
        }

        .action-content {
          display: grid;
          grid-template-columns: repeat(2, 1fr);
          gap: 12px;
          margin-left: 0;

          .el-button {
            border-radius: 8px;
            font-weight: 500;
            transition: all 0.3s ease;
            margin: 0;

            &:hover {
              transform: translateY(-2px);
              box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
            }
          }

          .el-divider {
            grid-column: span 2;
            margin: 4px 0;
          }
        }
      }
    }
  }

  .table-card {
    border-radius: 8px;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .form-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 5px;
  }

  .import-content {
    .upload-demo {
      margin: 20px 0;
    }

    .template-download {
      text-align: center;
      margin-top: 15px;
    }
  }

  .question-preview {
    .preview-header {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 20px;
      padding-bottom: 15px;
      border-bottom: 2px solid #ebeef5;

      .preview-score {
        margin-left: auto;
        font-size: 18px;
        font-weight: bold;
        color: #409eff;
      }
    }

    .preview-content,
    .preview-options,
    .preview-answer,
    .preview-analysis {
      margin-bottom: 20px;

      h4 {
        margin: 0 0 10px 0;
        color: #303133;
        font-size: 15px;
      }

      p {
        margin: 5px 0;
        line-height: 1.6;
        color: #606266;
      }
      
      .option-item {
        padding: 8px 12px;
        margin: 5px 0;
        background: #f5f7fa;
        border-radius: 4px;
        color: #606266;
        line-height: 1.6;
      }
    }
  }
}
</style>
