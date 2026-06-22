<template>
  <div class="review-container">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="editor-card">
          <template #header>
            <div class="card-header">
              <span>代码编辑器</span>
              <div class="header-actions">
                <el-input
                  v-model="fileName"
                  placeholder="文件名（可选）"
                  size="small"
                  style="width: 200px; margin-right: 10px;"
                />
                <el-select
                  v-if="classList.length > 0"
                  v-model="selectedClassId"
                  placeholder="选择班级规则"
                  size="small"
                  style="width: 150px; margin-right: 10px;"
                  clearable
                >
                  <el-option
                    v-for="cls in classList"
                    :key="cls.id"
                    :label="cls.className"
                    :value="cls.id"
                  />
                </el-select>
                <el-button type="primary" size="small" @click="handleSubmit" :loading="submitting">
                  提交审查
                </el-button>
              </div>
            </div>
          </template>
          <div ref="editorRef" class="editor-container"></div>
          <div class="editor-footer">
            <span class="line-count">行数: {{ lineCount }}</span>
            <span v-if="lineCount > 5000" class="line-warning">
              <el-icon><Warning /></el-icon>
              代码行数超出限制（5000行）
            </span>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card class="result-card">
          <template #header>
            <span>审查结果</span>
          </template>
          
          <div v-if="!reviewResult" class="empty-result">
            <el-empty description="提交代码开始审查" />
          </div>
          
          <div v-else class="review-result">
            <el-row :gutter="10" class="result-summary">
              <el-col :span="6">
                <div class="summary-item critical">
                  <div class="summary-value">{{ reviewResult.criticalCount }}</div>
                  <div class="summary-label">严重</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="summary-item error">
                  <div class="summary-value">{{ reviewResult.errorCount }}</div>
                  <div class="summary-label">错误</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="summary-item warning">
                  <div class="summary-value">{{ reviewResult.warningCount }}</div>
                  <div class="summary-label">警告</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="summary-item suggestion">
                  <div class="summary-value">{{ reviewResult.suggestionCount }}</div>
                  <div class="summary-label">建议</div>
                </div>
              </el-col>
            </el-row>
            
            <el-divider />
            
            <div class="issue-list">
              <el-scrollbar height="calc(100vh - 400px)">
                <div
                  v-for="(issue, index) in reviewResult.issues"
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
                  <div v-if="issue.codeBefore" class="issue-code">
                    <pre>{{ issue.codeBefore }}</pre>
                  </div>
                </div>
              </el-scrollbar>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import * as monaco from 'monaco-editor'
import { ElMessage } from 'element-plus'
import { submitReview } from '@/api/review'
import { getMyClasses } from '@/api/class'

const editorRef = ref()
let editor = null

const fileName = ref('')
const selectedClassId = ref(null)
const classList = ref([])
const submitting = ref(false)
const reviewResult = ref(null)

const lineCount = computed(() => {
  if (!editor) return 0
  const value = editor.getValue()
  return value ? value.split('\n').length : 0
})

onMounted(async () => {
  initEditor()
  await loadClasses()
})

onUnmounted(() => {
  if (editor) {
    editor.dispose()
  }
})

const initEditor = () => {
  editor = monaco.editor.create(editorRef.value, {
    value: '// 在此输入Java代码\npublic class HelloWorld {\n    public static void main(String[] args) {\n        System.out.println("Hello, World!");\n    }\n}',
    language: 'java',
    theme: 'vs',
    fontSize: 14,
    minimap: { enabled: false },
    automaticLayout: true,
    scrollBeyondLastLine: false,
    lineNumbers: 'on',
    renderLineHighlight: 'line',
    tabSize: 4,
    insertSpaces: true,
    wordWrap: 'off'
  })
}

const loadClasses = async () => {
  try {
    const res = await getMyClasses()
    classList.value = res.data
  } catch (error) {
    console.error('加载班级列表失败', error)
  }
}

const handleSubmit = async () => {
  if (!editor) return
  
  const code = editor.getValue()
  if (!code.trim()) {
    ElMessage.warning('请输入代码')
    return
  }
  
  if (lineCount.value > 5000) {
    ElMessage.error('代码行数超出限制（5000行）')
    return
  }
  
  submitting.value = true
  
  try {
    const res = await submitReview({
      codeContent: code,
      fileName: fileName.value || 'Untitled.java',
      classId: selectedClassId.value
    })
    
    reviewResult.value = res.data
    
    if (res.data.taskId) {
      ElMessage.info('代码行数较多，正在异步处理中...')
      pollAsyncResult(res.data.taskId)
    } else {
      ElMessage.success('审查完成')
    }
  } catch (error) {
    console.error('提交审查失败', error)
    ElMessage.error(error.message || '提交审查失败')
  } finally {
    submitting.value = false
  }
}

const pollAsyncResult = async (taskId) => {
  // 实现轮询逻辑
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
.review-container {
  height: calc(100vh - 120px);
}

.editor-card, .result-card {
  height: 100%;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.editor-container {
  height: calc(100% - 80px);
  min-height: 400px;
}

.editor-footer {
  padding: 10px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  .line-count {
    color: #666;
    font-size: 14px;
  }
  
  .line-warning {
    color: #f56c6c;
    font-size: 14px;
  }
}

.empty-result {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400px;
}

.result-summary {
  margin-bottom: 20px;
  
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
    
    .issue-code {
      margin-top: 10px;
      
      pre {
        background: #333;
        color: #fff;
        padding: 10px;
        border-radius: 4px;
        font-size: 12px;
        overflow-x: auto;
      }
    }
  }
}
</style>
