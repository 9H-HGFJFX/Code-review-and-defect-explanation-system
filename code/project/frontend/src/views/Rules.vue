<template>
  <div class="rules-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>审查规则管理</span>
          <el-button type="primary" @click="openCreateDialog">
            新增规则
          </el-button>
        </div>
      </template>
      
      <el-form :inline="true" class="filter-form">
        <el-form-item label="规则分类">
          <el-select v-model="filters.category" placeholder="全部" clearable>
            <el-option label="代码规范" value="STYLE" />
            <el-option label="代码缺陷" value="DEFECT" />
            <el-option label="安全漏洞" value="SECURITY" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.enabled" placeholder="全部" clearable>
            <el-option label="已启用" :value="1" />
            <el-option label="已禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
      
      <el-table :data="ruleList" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="规则名称" width="200" />
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            <el-tag :type="categoryType(row.category)" size="small">
              {{ categoryText(row.category) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="severity" label="严重级别" width="100">
          <template #default="{ row }">
            <el-tag :type="severityType(row.severity)" size="small">
              {{ severityText(row.severity) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="enabled" label="状态" width="80">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled === 1"
              @change="toggleRule(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-button type="danger" size="small" @click="deleteRule(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 20px; justify-content: flex-end;"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>
    
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="formRef" :model="ruleForm" :rules="formRules" label-width="100px">
        <el-form-item label="规则名称" prop="name">
          <el-input v-model="ruleForm.name" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="规则分类" prop="category">
          <el-select v-model="ruleForm.category" placeholder="请选择分类">
            <el-option label="代码规范" value="STYLE" />
            <el-option label="代码缺陷" value="DEFECT" />
            <el-option label="安全漏洞" value="SECURITY" />
          </el-select>
        </el-form-item>
        <el-form-item label="匹配模式" prop="patternType">
          <el-select v-model="ruleForm.patternType" placeholder="请选择匹配模式">
            <el-option label="正则表达式" value="REGEX" />
            <el-option label="AST语法树" value="AST" />
          </el-select>
        </el-form-item>
        <el-form-item label="匹配表达式" prop="pattern">
          <el-input v-model="ruleForm.pattern" placeholder="正则表达式或AST匹配规则" />
        </el-form-item>
        <el-form-item label="严重级别" prop="severity">
          <el-select v-model="ruleForm.severity" placeholder="请选择严重级别">
            <el-option label="严重" value="CRITICAL" />
            <el-option label="错误" value="ERROR" />
            <el-option label="警告" value="WARNING" />
            <el-option label="建议" value="SUGGESTION" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则描述">
          <el-input v-model="ruleForm.description" type="textarea" rows="3" />
        </el-form-item>
        <el-form-item label="建议模板">
          <el-input v-model="ruleForm.suggestionTemplate" type="textarea" rows="2" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRuleList, createRule, updateRule, toggleRule as apiToggleRule, deleteRule as apiDeleteRule } from '@/api/rule'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const ruleList = ref([])

const filters = reactive({
  category: '',
  enabled: null
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const ruleForm = reactive({
  name: '',
  category: '',
  patternType: 'REGEX',
  pattern: '',
  severity: '',
  description: '',
  suggestionTemplate: ''
})

const formRules = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择规则分类', trigger: 'change' }],
  patternType: [{ required: true, message: '请选择匹配模式', trigger: 'change' }],
  severity: [{ required: true, message: '请选择严重级别', trigger: 'change' }]
}

onMounted(() => {
  loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getRuleList({
      current: pagination.current,
      size: pagination.size,
      category: filters.category,
      enabled: filters.enabled
    })
    ruleList.value = res.data.records
    pagination.total = res.data.total
  } catch (error) {
    console.error('加载规则列表失败', error)
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  filters.category = ''
  filters.enabled = null
  loadData()
}

const openCreateDialog = () => {
  isEdit.value = false
  Object.assign(ruleForm, {
    name: '',
    category: '',
    patternType: 'REGEX',
    pattern: '',
    severity: '',
    description: '',
    suggestionTemplate: ''
  })
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  Object.assign(ruleForm, {
    id: row.id,
    name: row.name,
    category: row.category,
    patternType: row.patternType,
    pattern: row.pattern,
    severity: row.severity,
    description: row.description,
    suggestionTemplate: row.suggestionTemplate
  })
  dialogVisible.value = true
}

const submitForm = async () => {
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateRule(ruleForm.id, ruleForm)
      ElMessage.success('更新成功')
    } else {
      await createRule(ruleForm)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const toggleRule = async (row) => {
  try {
    await apiToggleRule(row.id, row.enabled !== 1)
    ElMessage.success(row.enabled === 1 ? '已禁用' : '已启用')
    loadData()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const deleteRule = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该规则吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await apiDeleteRule(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const dialogTitle = computed(() => isEdit.value ? '编辑规则' : '新增规则')

const categoryType = (category) => {
  switch (category) {
    case 'STYLE': return ''
    case 'DEFECT': return 'warning'
    case 'SECURITY': return 'danger'
    default: return 'info'
  }
}

const categoryText = (category) => {
  switch (category) {
    case 'STYLE': return '代码规范'
    case 'DEFECT': return '代码缺陷'
    case 'SECURITY': return '安全漏洞'
    default: return category
  }
}

const severityType = (severity) => {
  switch (severity) {
    case 'CRITICAL': return 'danger'
    case 'ERROR': return 'warning'
    case 'WARNING': return 'info'
    case 'SUGGESTION': return 'success'
    default: return 'info'
  }
}

const severityText = (severity) => {
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
.rules-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .filter-form {
    margin-bottom: 20px;
  }
}
</style>
