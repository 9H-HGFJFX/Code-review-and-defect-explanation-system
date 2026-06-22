<template>
  <div class="history-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>审查历史</span>
          <el-button type="primary" @click="$router.push('/review')">
            新建审查
          </el-button>
        </div>
      </template>
      
      <el-table :data="reviewList" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="fileName" label="文件名" width="200" />
        <el-table-column prop="lineCount" label="代码行数" width="100" />
        <el-table-column prop="totalIssues" label="问题数" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.totalIssues > 0" type="danger" size="small">
              {{ row.totalIssues }}
            </el-tag>
            <el-tag v-else type="success" size="small">0</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewTime" label="审查时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewDetail(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 20px; justify-content: flex-end;"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>
    
    <el-dialog v-model="detailVisible" title="审查详情" width="70%" top="5vh">
      <div v-if="currentReview" class="detail-content">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="文件名">{{ currentReview.fileName }}</el-descriptions-item>
          <el-descriptions-item label="代码行数">{{ currentReview.lineCount }}</el-descriptions-item>
          <el-descriptions-item label="审查时间">{{ currentReview.reviewTime }}</el-descriptions-item>
        </el-descriptions>
        
        <el-divider />
        
        <el-row :gutter="10" class="result-summary">
          <el-col :span="6">
            <div class="summary-item critical">
              <div class="summary-value">{{ currentReview.criticalCount }}</div>
              <div class="summary-label">严重</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="summary-item error">
              <div class="summary-value">{{ currentReview.errorCount }}</div>
              <div class="summary-label">错误</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="summary-item warning">
              <div class="summary-value">{{ currentReview.warningCount }}</div>
              <div class="summary-label">警告</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="summary-item suggestion">
              <div class="summary-value">{{ currentReview.suggestionCount }}</div>
              <div class="summary-label">建议</div>
            </div>
          </el-col>
        </el-row>
        
        <el-divider />
        
        <div class="issue-list">
          <el-scrollbar height="400px">
            <div
              v-for="(issue, index) in currentReview.issues"
              :key="index"
              class="issue-item"
              :class="getSeverityClass(issue.severity)"
            >
              <div class="issue-header">
                <el-tag :type="getSeverityTagType(issue.severity)" size="small">
                  {{ getSeverityText(issue.severity) }}
                </el-tag>
                <span class="issue-line">行 {{ issue.lineNumber }}</span>
              </div>
              <div class="issue-description">{{ issue.description }}</div>
              <div class="issue-suggestion">
                <el-icon><InfoFilled /></el-icon>
                {{ issue.suggestion }}
              </div>
            </div>
          </el-scrollbar>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getReviewList, getReviewResult } from '@/api/review'

const loading = ref(false)
const reviewList = ref([])
const detailVisible = ref(false)
const currentReview = ref(null)

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

onMounted(() => {
  loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getReviewList({
      current: pagination.current,
      size: pagination.size
    })
    reviewList.value = res.data.records
    pagination.total = res.data.total
  } catch (error) {
    console.error('加载审查历史失败', error)
  } finally {
    loading.value = false
  }
}

const viewDetail = async (row) => {
  try {
    const res = await getReviewResult(row.id)
    currentReview.value = res.data
    detailVisible.value = true
  } catch (error) {
    console.error('加载审查详情失败', error)
  }
}

const statusType = (status) => {
  switch (status) {
    case 'COMPLETED': return 'success'
    case 'PENDING': return 'warning'
    case 'FAILED': return 'danger'
    default: return 'info'
  }
}

const statusText = (status) => {
  switch (status) {
    case 'COMPLETED': return '已完成'
    case 'PENDING': return '处理中'
    case 'FAILED': return '失败'
    default: return status
  }
}

const getSeverityClass = (severity) => {
  switch (severity) {
    case 'CRITICAL': return 'severity-critical'
    case 'ERROR': return 'severity-error'
    case 'WARNING': return 'severity-warning'
    case 'SUGGESTION': return 'severity-suggestion'
    default: return ''
  }
}

const getSeverityTagType = (severity) => {
  switch (severity) {
    case 'CRITICAL': return 'danger'
    case 'ERROR': return 'warning'
    case 'WARNING': return 'info'
    case 'SUGGESTION': return 'success'
    default: return 'info'
  }
}

const getSeverityText = (severity) => {
  switch (severity) {
    case 'CRITICAL': return '严重'
    case 'ERROR': return '错误'
    case 'WARNING': return '警告'
    case 'SUGGESTION': return '建议'
    default: return severity
  }
}
</script>

<style scoped lang="scss">
.history-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.detail-content {
  .result-summary {
    margin: 20px 0;
    
    .summary-item {
      text-align: center;
      padding: 15px;
      border-radius: 8px;
      
      .summary-value {
        font-size: 24px;
        font-weight: bold;
      }
      
      .summary-label {
        font-size: 12px;
        margin-top: 5px;
      }
      
      &.critical { background: #fef0f0; color: #f56c6c; }
      &.error { background: #fdf6ec; color: #e6a23c; }
      &.warning { background: #ecf5ff; color: #409eff; }
      &.suggestion { background: #f0f9eb; color: #67c23a; }
    }
  }
}

.issue-list {
  .issue-item {
    padding: 15px;
    margin-bottom: 10px;
    border-radius: 8px;
    border-left: 4px solid;
    
    &.severity-critical { background: #fef0f0; border-color: #f56c6c; }
    &.severity-error { background: #fdf6ec; border-color: #e6a23c; }
    &.severity-warning { background: #ecf5ff; border-color: #409eff; }
    &.severity-suggestion { background: #f0f9eb; border-color: #67c23a; }
    
    .issue-header {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 10px;
      
      .issue-line {
        color: #666;
        font-size: 12px;
      }
    }
    
    .issue-description {
      font-size: 14px;
      margin-bottom: 10px;
    }
    
    .issue-suggestion {
      font-size: 12px;
      color: #666;
      background: rgba(255,255,255,0.5);
      padding: 8px;
      border-radius: 4px;
      
      .el-icon {
        margin-right: 5px;
      }
    }
  }
}
</style>
