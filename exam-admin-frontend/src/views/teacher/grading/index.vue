<template>
  <div class="grading-management-container">
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item :to="{ path: '/teacher/dashboard' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>阅卷与成绩</el-breadcrumb-item>
    </el-breadcrumb>

    <el-tabs v-model="activeTab" type="card">
      <!-- 待阅卷 -->
      <el-tab-pane label="待阅卷" name="pending">
        <el-card class="tab-card full-height-card" shadow="never">
          <div class="pending-table-wrapper">
            <el-table :data="pendingList" v-loading="loading" style="width: 100%" :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
              <el-table-column prop="examName" label="考试名称" min-width="200" />
              <el-table-column prop="className" label="班级" width="150" />
              <el-table-column label="待阅人数" width="120">
                <template #default="{row}"><el-tag type="danger">{{row.pendingCount}}</el-tag></template>
              </el-table-column>
              <el-table-column prop="submitTime" label="提交时间" width="180" />
              <el-table-column label="操作" min-width="200" fixed="right">
                <template #default="{row}"><el-button type="primary" @click="handleGrade(row)">开始阅卷</el-button><el-button type="warning" @click="handleRollbackPending(row)">打回</el-button></template>
              </el-table-column>
            </el-table>
          </div>
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="pendingPage"
              v-model:page-size="pendingPageSize"
              :total="pendingTotal"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handlePendingSizeChange"
              @current-change="handlePendingPageChange"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 已阅卷 -->
      <el-tab-pane label="已阅卷" name="graded">
        <el-card class="tab-card full-height-card" shadow="never">
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
          
          <div class="graded-table-wrapper">
            <el-table :data="gradedList" v-loading="loading" style="width: 100%" :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
              <el-table-column prop="studentNo" label="学号" min-width="120" />
              <el-table-column prop="realName" label="姓名" min-width="100" />
              <el-table-column label="客观题得分" min-width="120"><template #default="{row}">{{row.objectiveScore}}/{{row.objectiveTotal}}</template></el-table-column>
              <el-table-column label="主观题得分" min-width="120"><template #default="{row}">{{row.subjectiveScore}}/{{row.subjectiveTotal}}</template></el-table-column>
              <el-table-column prop="totalScore" label="总分" min-width="100"><template #default="{row}"><span style="color:#67c23a;font-weight:bold">{{row.totalScore}}</span></template></el-table-column>
              <el-table-column label="操作" min-width="200"><template #default="{row}"><el-button type="primary" link @click="handleViewDetail(row)">详情</el-button><el-button type="warning" link @click="handleRollback(row)">打回</el-button></template></el-table-column>
            </el-table>
          </div>
          
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="gradedPage"
              v-model:page-size="gradedPageSize"
              :total="gradedTotal"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleGradedSizeChange"
              @current-change="handleGradedPageChange"
            />
          </div>
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

      <!-- 班级对比 -->
      <el-tab-pane label="班级对比" name="class-comparison">
        <el-card class="tab-card" shadow="never">
          <el-form :inline="true">
            <el-form-item label="考试">
              <el-select v-model="classForm.examId" style="width:180px" placeholder="请选择考试" @change="fetchClassComparison">
                <el-option 
                  v-for="exam in examOptions" 
                  :key="exam.id" 
                  :label="exam.examName" 
                  :value="exam.id"
                />
              </el-select>
            </el-form-item>
          </el-form>

          <el-table :data="classComparisonData" style="margin-top:20px" v-loading="classLoading">
            <el-table-column prop="className" label="班级" width="200" />
            <el-table-column prop="studentCount" label="参考人数" width="100" />
            <el-table-column prop="avgScore" label="平均分" width="100">
              <template #default="{row}"><span style="font-weight:bold;color:#409eff">{{row.avgScore}}</span></template>
            </el-table-column>
            <el-table-column prop="maxScore" label="最高分" width="100" />
            <el-table-column prop="minScore" label="最低分" width="100" />
            <el-table-column prop="passRate" label="及格率" width="100">
              <template #default="{row}"><el-tag :type="row.passRate >= 60 ? 'success' : 'danger'">{{row.passRate}}%</el-tag></template>
            </el-table-column>
            <el-table-column label="成绩分布" min-width="200">
              <template #default="{row}">
                <el-progress :percentage="Math.round(row.avgScore)" :color="getProgressColor(row.avgScore)" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 知识点分析 -->
      <el-tab-pane label="知识点分析" name="knowledge-analysis">
        <el-card class="tab-card" shadow="never">
          <el-form :inline="true">
            <el-form-item label="考试">
              <el-select v-model="knowledgeForm.examId" style="width:180px" placeholder="请选择考试" @change="fetchKnowledgeAnalysis">
                <el-option 
                  v-for="exam in examOptions" 
                  :key="exam.id" 
                  :label="exam.examName" 
                  :value="exam.id"
                />
              </el-select>
            </el-form-item>
          </el-form>

          <el-table :data="knowledgeData" style="margin-top:20px" v-loading="knowledgeLoading">
            <el-table-column prop="knowledgePoint" label="知识点" min-width="200" show-overflow-tooltip />
            <el-table-column prop="totalCount" label="题目数" width="100" align="center" />
            <el-table-column prop="correctCount" label="答对数" width="100" align="center" />
            <el-table-column prop="accuracy" label="掌握度" width="180">
              <template #default="{row}">
                <el-progress :percentage="row.accuracy" :color="getAccuracyColor(row.accuracy)" />
              </template>
            </el-table-column>
            <el-table-column label="掌握情况" width="120" align="center">
              <template #default="{row}">
                <el-tag :type="getAccuracyType(row.accuracy)">
                  {{ getAccuracyText(row.accuracy) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 试题质量分析 -->
      <el-tab-pane label="试题质量" name="question-quality">
        <el-card class="tab-card" shadow="never">
          <el-form :inline="true">
            <el-form-item label="考试">
              <el-select v-model="qualityForm.examId" style="width:180px" placeholder="请选择考试" @change="fetchQuestionQuality">
                <el-option 
                  v-for="exam in examOptions" 
                  :key="exam.id" 
                  :label="exam.examName" 
                  :value="exam.id"
                />
              </el-select>
            </el-form-item>
          </el-form>

          <el-row :gutter="20" style="margin-top:20px" v-if="qualitySummary">
            <el-col :span="8">
              <el-card shadow="never">
                <div style="text-align:center">
                  <div style="font-size:12px;color:#909399">平均难度系数</div>
                  <div style="font-size:28px;font-weight:bold;color:#409eff">{{qualitySummary.avgDifficulty}}%</div>
                  <div style="font-size:12px;color:#606266">{{getDifficultyLevelText(qualitySummary.avgDifficulty)}}</div>
                </div>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card shadow="never">
                <div style="text-align:center">
                  <div style="font-size:12px;color:#909399">平均区分度</div>
                  <div style="font-size:28px;font-weight:bold;color:#67c23a">{{qualitySummary.avgDiscrimination}}%</div>
                  <div style="font-size:12px;color:#606266">{{getDiscriminationLevelText(qualitySummary.avgDiscrimination)}}</div>
                </div>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card shadow="never">
                <div style="text-align:center">
                  <div style="font-size:12px;color:#909399">分析题目数</div>
                  <div style="font-size:28px;font-weight:bold;color:#e6a23c">{{qualitySummary.totalQuestions}}</div>
                  <div style="font-size:12px;color:#606266">道客观题</div>
                </div>
              </el-card>
            </el-col>
          </el-row>

          <el-table :data="questionQualityData" style="margin-top:20px" v-loading="qualityLoading">
            <el-table-column prop="content" label="题目内容" show-overflow-tooltip min-width="300" />
            <el-table-column prop="difficulty" label="难度系数" width="100">
              <template #default="{row}">
                <el-tag :type="getDifficultyType(row.difficulty)">{{row.difficulty}}%</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="difficultyLevel" label="难度等级" width="100" />
            <el-table-column prop="discrimination" label="区分度" width="100">
              <template #default="{row}">
                <el-tag :type="getDiscriminationType(row.discrimination)">{{row.discrimination}}%</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="discriminationLevel" label="区分度评价" width="100" />
            <el-table-column prop="correctCount" label="答对数" width="80" />
            <el-table-column prop="totalAnswered" label="答题数" width="80" />
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" :title="'答题详情 - ' + detailData.studentName" width="900px">
      <el-descriptions :column="2" border style="margin-bottom: 20px">
        <el-descriptions-item label="学号">{{ detailData.studentNo }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ detailData.studentName }}</el-descriptions-item>
        <el-descriptions-item label="客观题得分">{{ detailData.objectiveScore }}/{{ detailData.objectiveTotal }}</el-descriptions-item>
        <el-descriptions-item label="主观题得分">{{ detailData.subjectiveScore }}/{{ detailData.subjectiveTotal }}</el-descriptions-item>
        <el-descriptions-item label="总分" :span="2">
          <span style="color:#67c23a;font-weight:bold;font-size:18px">{{ detailData.totalScore }}</span> 分
        </el-descriptions-item>
      </el-descriptions>
      
      <el-tabs type="border-card">
        <el-tab-pane label="客观题">
          <el-table :data="detailData.objectiveQuestions" style="width: 100%">
            <el-table-column prop="number" label="题号" width="80" />
            <el-table-column prop="type" label="题型" width="100" />
            <el-table-column prop="content" label="题目内容" show-overflow-tooltip />
            <el-table-column label="正确答案" width="100">
              <template #default="{row}">
                <span style="color:#67c23a">{{ row.correctAnswer }}</span>
              </template>
            </el-table-column>
            <el-table-column label="学生答案" width="100">
              <template #default="{row}">
                <span :style="{color: row.isCorrect ? '#67c23a' : '#f56c6c'}">{{ row.studentAnswer }}</span>
              </template>
            </el-table-column>
            <el-table-column label="得分" width="80">
              <template #default="{row}">
                <span :style="{color: row.isCorrect ? '#67c23a' : '#f56c6c', fontWeight: 'bold'}">
                  {{ row.isCorrect ? row.fullScore : 0 }}/{{ row.fullScore }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        
        <el-tab-pane label="主观题">
          <div v-for="q in detailData.subjectiveQuestions" :key="q.id" class="question-detail-item">
            <div class="question-detail-header">
              <span class="question-number">{{ q.number }}.</span>
              <span class="question-type">{{ q.type }}</span>
              <span class="question-score">得分: {{ q.score }}/{{ q.fullScore }}分</span>
            </div>
            <div class="question-detail-content">
              <h5>题目内容:</h5>
              <p>{{ q.content }}</p>
            </div>
            <div class="answer-detail">
              <h5>学生答案:</h5>
              <p style="background:#f9f9f9;padding:10px;border-radius:4px">{{ q.studentAnswer }}</p>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
      
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 阅卷对话框 -->
    <el-dialog v-model="gradeDialogVisible" title="在线阅卷" width="1000px" top="5vh">
      <div class="grading-content">
        <el-alert type="info" :closable="false">
          <template #default>
            客观题已自动批改，请对主观题进行评分。
            <span v-if="currentRecord.studentName">当前学生：<strong>{{currentRecord.studentName}}</strong> ({{currentRecord.studentNo}})</span>
          </template>
        </el-alert>
        
        <!-- 学生信息 -->
        <el-descriptions :column="3" border style="margin-top:15px;margin-bottom:15px">
          <el-descriptions-item label="学号">{{currentRecord.studentNo}}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{currentRecord.studentName}}</el-descriptions-item>
          <el-descriptions-item label="待评题目">
            <el-tag type="warning">{{subjectiveQuestions.length}}题</el-tag>
          </el-descriptions-item>
        </el-descriptions>
        
        <div class="question-list">
          <div v-for="q in subjectiveQuestions" :key="q.id" class="question-item">
            <div class="question-header">
              <span class="question-number">{{q.number}}.</span>
              <span class="question-type">{{q.type}}</span>
              <span class="question-score">满分: {{q.fullScore}}分</span>
            </div>
            
            <div class="question-section">
              <h5><el-icon><Document /></el-icon> 题目内容:</h5>
              <p class="content-text">{{q.content}}</p>
            </div>
            
            <div class="reference-answer-section" v-if="q.referenceAnswer">
              <h5><el-icon><Reading /></el-icon> 参考答案:</h5>
              <div class="answer-text">{{q.referenceAnswer}}</div>
            </div>
            
            <div class="student-answer-section">
              <h5><el-icon><User /></el-icon> 学生答案:</h5>
              <div class="answer-text student-text">{{q.studentAnswer || '(未作答)'}}</div>
            </div>
            
            <div class="grading-section">
              <div class="grading-left">
                <h5>评分:</h5>
                <el-input-number v-model="q.score" :min="0" :max="q.fullScore" size="large" :precision="0" />
                <span class="score-text">/ {{q.fullScore}}分</span>
              </div>
              <div class="grading-right">
                <el-input 
                  v-model="q.comment" 
                  type="textarea" 
                  :rows="2" 
                  placeholder="请输入评分评语（可选，如：思路清晰、步骤完整等）" 
                />
              </div>
            </div>
          </div>
        </div>
        
        <!-- 评分汇总 -->
        <div class="grading-summary">
          <el-divider />
          <div class="summary-row">
            <span>当前总分:</span>
            <span class="total-score">{{subjectiveQuestions.reduce((sum, q) => sum + (q.score || 0), 0)}}分</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="gradeDialogVisible=false">取消</el-button>
        <el-button type="success" @click="saveGrade" :loading="saving">保存进度</el-button>
        <el-button type="primary" @click="submitGrade" :loading="submitting">提交评分</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Reading, User } from '@element-plus/icons-vue'
import { 
  getPendingGrading, 
  getGradedRecords, 
  getGradingDetail, 
  submitGrading,
  getScoreStatistics,
  getErrorAnalysis,
  exportExamScores,
  getExamList,
  getMyClasses,
  getClassComparison,
  getKnowledgeAnalysis,
  getQuestionQuality,
  rollbackExamRecord
} from '@/api/teacher/index'

const activeTab = ref('pending')
const loading = ref(false)
const pendingList = ref([])
const gradedList = ref([])
// 待阅卷分页
const pendingPage = ref(1)
const pendingPageSize = ref(10)
const pendingTotal = ref(0)
// 已阅卷分页
const gradedPage = ref(1)
const gradedPageSize = ref(10)
const gradedTotal = ref(0)
const gradeDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const detailData = reactive({
  studentNo: '',
  studentName: '',
  objectiveScore: 0,
  objectiveTotal: 0,
  subjectiveScore: 0,
  subjectiveTotal: 0,
  totalScore: 0,
  objectiveQuestions: [],
  subjectiveQuestions: []
})
const currentRecord = reactive({
  recordId: null,
  studentName: '',
  studentNo: ''
})
const subjectiveQuestions = ref([])
const submitting = ref(false)
const saving = ref(false)
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

// 班级对比相关
const classForm = reactive({ examId: null })
const classComparisonData = ref([])
const classLoading = ref(false)

// 知识点分析相关
const knowledgeForm = reactive({ examId: null })
const knowledgeData = ref([])
const knowledgeLoading = ref(false)

// 试题质量分析相关
const qualityForm = reactive({ examId: null })
const questionQualityData = ref([])
const qualitySummary = ref(null)
const qualityLoading = ref(false)

// 考试和班级选项
const examOptions = ref([])
const classOptions = ref([])

// 获取待阅卷列表
const fetchPendingGrading = async () => {
  loading.value = true
  try {
    const res = await getPendingGrading({
      pageNum: pendingPage.value,
      pageSize: pendingPageSize.value
    })
    if (res.data) {
      // 支持分页数据格式
      if (res.data.records) {
        pendingList.value = res.data.records
        pendingTotal.value = res.data.total || 0
      } else if (Array.isArray(res.data)) {
        pendingList.value = res.data
        pendingTotal.value = res.data.length
      }
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
    const params = {
      pageNum: gradedPage.value,
      pageSize: gradedPageSize.value
    }
    if (searchForm.examId) params.examId = searchForm.examId
    if (searchForm.classId) params.classId = searchForm.classId
    
    const res = await getGradedRecords(params)
    if (res.data) {
      // 支持分页数据格式
      if (res.data.records) {
        gradedList.value = res.data.records
        gradedTotal.value = res.data.total || 0
      } else if (Array.isArray(res.data)) {
        gradedList.value = res.data
        gradedTotal.value = res.data.length
      }
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
  gradedPage.value = 1
  fetchGradedRecords()
}

// 待阅卷分页处理
const handlePendingSizeChange = (size) => {
  pendingPageSize.value = size
  fetchPendingGrading()
}

const handlePendingPageChange = (page) => {
  pendingPage.value = page
  fetchPendingGrading()
}

// 已阅卷分页处理
const handleGradedSizeChange = (size) => {
  gradedPageSize.value = size
  fetchGradedRecords()
}

const handleGradedPageChange = (page) => {
  gradedPage.value = page
  fetchGradedRecords()
}

// 开始阅卷
const handleGrade = async (row) => {
  try {
    const res = await getGradingDetail(row.recordId)
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

// 保存评分进度
const saveGrade = async () => {
  saving.value = true
  try {
    const data = {
      recordId: currentRecord.recordId,
      subjectiveQuestions: subjectiveQuestions.value.map(q => ({
        questionId: q.id,
        score: q.score || 0,
        comment: q.comment || ''
      })),
      subjectiveScore: subjectiveQuestions.value.reduce((sum, q) => sum + (q.score || 0), 0)
    }
    await submitGrading(data)
    ElMessage.success('评分进度已保存')
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 提交阅卷
const submitGrade = async () => {
  // 验证是否所有题目都已评分
  const ungraded = subjectiveQuestions.value.filter(q => q.score === null || q.score === undefined)
  if (ungraded.length > 0) {
    try {
      await ElMessageBox.confirm(
        `还有 ${ungraded.length} 道题目未评分，确定要提交吗？未评分的题目将计为0分。`,
        '提示',
        { confirmButtonText: '确定提交', cancelButtonText: '继续评分', type: 'warning' }
      )
    } catch {
      return // 用户取消
    }
  }
  
  submitting.value = true
  try {
    const data = {
      recordId: currentRecord.recordId,
      subjectiveQuestions: subjectiveQuestions.value.map(q => ({
        questionId: q.id,
        score: q.score || 0,
        comment: q.comment || ''
      })),
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
  } finally {
    submitting.value = false
  }
}

// 查看详情
const handleViewDetail = async (row) => {
  try {
    const res = await getGradingDetail(row.id)
    if (res.data) {
      detailData.studentNo = res.data.studentNo
      detailData.studentName = res.data.studentName
      detailData.objectiveScore = res.data.objectiveScore
      detailData.objectiveTotal = res.data.objectiveTotal || 0
      detailData.subjectiveScore = res.data.subjectiveScore || 0
      detailData.subjectiveTotal = res.data.subjectiveTotal || 0
      detailData.totalScore = res.data.totalScore
      detailData.objectiveQuestions = res.data.objectiveQuestions || []
      detailData.subjectiveQuestions = res.data.subjectiveQuestions || []
      detailDialogVisible.value = true
    }
  } catch (error) {
    console.error('获取详情失败:', error)
    ElMessage.error('获取详情失败')
  }
}

// 打回重做（已阅卷列表）
const handleRollback = async (row) => {
  try {
    await ElMessageBox.prompt('请输入打回原因（选填）', '打回重做', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：成绩异常，请重新考试',
      inputPattern: /.{0,200}/,
      inputErrorMessage: '原因不能超过200字'
    }).then(async ({ value }) => {
      await rollbackExamRecord(row.id, value || '')
      ElMessage.success('已打回，学生可以重新考试')
      fetchGradedRecords()
      fetchPendingGrading()
    })
  } catch (error) {
    if (error !== 'cancel') {
      console.error('打回失败:', error)
      ElMessage.error('打回失败')
    }
  }
}

// 打回重做（待阅卷列表）
const handleRollbackPending = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要打回该学生的考试吗？打回后学生可以重新答题。`, '打回重做', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      await rollbackExamRecord(row.recordId, '')
      ElMessage.success('已打回，学生可以重新考试')
      fetchGradedRecords()
      fetchPendingGrading()
    })
  } catch (error) {
    if (error !== 'cancel') {
      console.error('打回失败:', error)
      ElMessage.error('打回失败')
    }
  }
}

// 获取考试列表
const fetchExams = async () => {
  try {
    const res = await getExamList({ pageNum: 1, pageSize: 100 })
    if (res.data) {
      // 后端返回的是数组格式
      if (Array.isArray(res.data)) {
        examOptions.value = res.data.map(exam => ({
          id: exam.id,
          examName: exam.examName
        }))
      } else if (res.data.records) {
        // 兼容PageResult格式
        examOptions.value = res.data.records.map(exam => ({
          id: exam.id,
          examName: exam.examName
        }))
      }
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
    const res = await exportExamScores({ examId: statsForm.examId })
    
    // 检查是否返回了错误（错误时返回的是JSON格式的blob）
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
    link.download = '成绩导出_' + new Date().getTime() + '.xlsx'
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

// 获取成绩统计
const fetchStatistics = async () => {
  if (!statsForm.examId) {
    ElMessage.warning('请选择考试')
    return
  }
  
  try {
    // 获取统计数据
    const res = await getScoreStatistics(statsForm.examId)
    if (res.data) {
      Object.assign(statistics, res.data)
    }
    
    // 获取错题分析
    const errorRes = await getErrorAnalysis(statsForm.examId)
    if (errorRes.data) {
      errorQuestions.value = errorRes.data
    }
  } catch (error) {
    console.error('获取成绩统计失败:', error)
    ElMessage.error('获取成绩统计失败')
  }
}

// 获取班级对比
const fetchClassComparison = async () => {
  if (!classForm.examId) return
  classLoading.value = true
  try {
    const res = await getClassComparison(classForm.examId)
    if (res.data) {
      classComparisonData.value = res.data
    }
  } catch (error) {
    console.error('获取班级对比失败:', error)
    ElMessage.error('获取班级对比失败')
  } finally {
    classLoading.value = false
  }
}

// 获取知识点分析
const fetchKnowledgeAnalysis = async () => {
  if (!knowledgeForm.examId) return
  knowledgeLoading.value = true
  try {
    const res = await getKnowledgeAnalysis(knowledgeForm.examId)
    if (res.data && res.data.knowledgeList) {
      knowledgeData.value = res.data.knowledgeList
    }
  } catch (error) {
    console.error('获取知识点分析失败:', error)
    ElMessage.error('获取知识点分析失败')
  } finally {
    knowledgeLoading.value = false
  }
}

// 获取试题质量分析
const fetchQuestionQuality = async () => {
  if (!qualityForm.examId) return
  qualityLoading.value = true
  try {
    const res = await getQuestionQuality(qualityForm.examId)
    if (res.data) {
      questionQualityData.value = res.data.questions || []
      qualitySummary.value = {
        avgDifficulty: res.data.avgDifficulty,
        avgDiscrimination: res.data.avgDiscrimination,
        totalQuestions: res.data.totalQuestions
      }
    }
  } catch (error) {
    console.error('获取试题质量分析失败:', error)
    ElMessage.error('获取试题质量分析失败')
  } finally {
    qualityLoading.value = false
  }
}

// 辅助方法：获取进度条颜色
const getProgressColor = (score) => {
  if (score >= 90) return '#67c23a'
  if (score >= 80) return '#409eff'
  if (score >= 70) return '#e6a23c'
  if (score >= 60) return '#909399'
  return '#f56c6c'
}

// 辅助方法：获取掌握度颜色
const getAccuracyColor = (accuracy) => {
  if (accuracy >= 80) return '#67c23a'
  if (accuracy >= 60) return '#409eff'
  if (accuracy >= 40) return '#e6a23c'
  return '#f56c6c'
}

// 辅助方法：获取掌握度文本
const getAccuracyText = (accuracy) => {
  if (accuracy >= 80) return '优秀'
  if (accuracy >= 60) return '良好'
  if (accuracy >= 40) return '一般'
  return '较差'
}

// 辅助方法：获取掌握度类型
const getAccuracyType = (accuracy) => {
  if (accuracy >= 80) return 'success'
  if (accuracy >= 60) return ''
  if (accuracy >= 40) return 'warning'
  return 'danger'
}

// 辅助方法：获取难度等级文本（用于卡片显示）
const getDifficultyLevelText = (difficulty) => {
  if (difficulty >= 70) return '简单'
  if (difficulty >= 40) return '中等'
  return '困难'
}

// 辅助方法：获取区分度评价文本（用于卡片显示）
const getDiscriminationLevelText = (discrimination) => {
  if (discrimination >= 40) return '优秀'
  if (discrimination >= 30) return '良好'
  if (discrimination >= 20) return '一般'
  return '较差'
}

// 辅助方法：获取难度等级（用于表格显示）
const getDifficultyLevel = (difficulty) => {
  if (difficulty >= 70) return '简单'
  if (difficulty >= 40) return '中等'
  return '困难'
}

// 辅助方法：获取难度类型
const getDifficultyType = (difficulty) => {
  if (difficulty >= 70) return 'success'
  if (difficulty >= 40) return ''
  return 'warning'
}

// 辅助方法：获取区分度评价
const getDiscriminationLevel = (discrimination) => {
  if (discrimination >= 40) return '优秀'
  if (discrimination >= 30) return '良好'
  if (discrimination >= 20) return '一般'
  return '较差'
}

// 辅助方法：获取区分度类型
const getDiscriminationType = (discrimination) => {
  if (discrimination >= 40) return 'success'
  if (discrimination >= 30) return ''
  if (discrimination >= 20) return 'warning'
  return 'danger'
}

onMounted(() => {
  fetchPendingGrading()
  fetchGradedRecords()
  fetchExams()
  fetchClasses()
})

// 监听标签页切换
watch(activeTab, (newVal) => {
  if (newVal === 'graded') {
    fetchGradedRecords()
  } else if (newVal === 'pending') {
    fetchPendingGrading()
  }
})
</script>

<style scoped>
.grading-management-container { 
  padding: 20px; 
  background: #f5f7fa; 
  min-height: calc(100vh - 56px);
  height: calc(100vh - 56px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.breadcrumb { margin-bottom: 20px; }
.tab-card { 
  border-radius: 8px; 
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.full-height-card {
  height: calc(100vh - 180px);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}
.full-height-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 20px;
  height: 100%;
  min-height: 0;
}

/* 已阅卷表格容器 */
.graded-table-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  margin-top: 15px;
  min-height: 0;
  width: 100%;
  height: 100%;
}

.graded-table-wrapper :deep(.el-table) {
  flex: 1;
  min-height: 0;
  width: 100% !important;
  height: 100% !important;
  overflow-y: auto;
  display: block;
}

/* 已阅卷表格固定高度 */
.graded-table-wrapper :deep(.el-table__body-wrapper) {
  flex: 1;
  min-height: 0;
}

/* 待阅卷表格容器 */
.pending-table-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  width: 100%;
  height: 100%;
}

.pending-table-wrapper :deep(.el-table) {
  flex: 1;
  min-height: 0;
  width: 100% !important;
  height: 100% !important;
  display: block;
}

/* 待阅卷表格固定高度 */
.pending-table-wrapper :deep(.el-table__body-wrapper) {
  flex: 1;
  min-height: 0;
}

.pagination-wrapper {
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: center;
  flex-shrink: 0;
}
.search-form { margin-bottom: 10px; }

/* 阅卷对话框样式 */
.grading-content .question-list { margin-top: 20px; max-height: 60vh; overflow-y: auto; }
.question-item { 
  padding: 20px; 
  margin-bottom: 20px; 
  border: 1px solid #ebeef5; 
  border-radius: 8px;
  background: #fff;
}
.question-header { 
  display: flex; 
  align-items: center; 
  gap: 10px; 
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 2px solid #409eff;
}
.question-number { font-weight: bold; font-size: 16px; color: #303133; }
.question-type { color: #909399; font-size: 14px; }
.question-score { margin-left: auto; color: #409eff; font-weight: bold; }

.question-section, .reference-answer-section, .student-answer-section {
  margin-bottom: 15px;
}
.question-section h5, .reference-answer-section h5, .student-answer-section h5 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 5px;
}
.content-text, .answer-text {
  line-height: 1.8;
  padding: 12px;
  border-radius: 6px;
  background: #f5f7fa;
  font-size: 14px;
  white-space: pre-wrap;
  word-break: break-word;
}
.student-text {
  background: #fef0f0;
  border-left: 3px solid #f56c6c;
}
.reference-answer-section .answer-text {
  background: #f0f9ff;
  border-left: 3px solid #409eff;
}

.grading-section {
  display: flex;
  gap: 20px;
  padding: 15px;
  background: linear-gradient(to right, #f9f9f9, #ffffff);
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}
.grading-left {
  flex: 0 0 250px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.grading-left h5 {
  margin: 0;
  white-space: nowrap;
}
.score-text {
  color: #909399;
  font-size: 14px;
}
.grading-right {
  flex: 1;
}

/* 评分汇总 */
.grading-summary {
  margin-top: 20px;
}
.summary-row {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 15px;
  font-size: 16px;
}
.total-score {
  font-size: 24px;
  font-weight: bold;
  color: #67c23a;
}

/* 详情对话框样式 */
.question-detail-item {
  margin-bottom: 20px;
  padding: 15px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}
.question-detail-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}
.question-detail-content h5,
.answer-detail h5 {
  margin: 10px 0 5px 0;
  font-size: 14px;
  color: #606266;
}
</style>
