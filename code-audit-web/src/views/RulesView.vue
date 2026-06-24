<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Edit, Delete } from '@element-plus/icons-vue'
import { ruleApi } from '@/api/rule'
import type { RuleVO, RuleSaveReq } from '@/types/api'

const list = ref<RuleVO[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(20)
const category = ref<string>('')
const loading = ref(false)

const dialogVisible = ref(false)
const dialogMode = ref<'add' | 'edit'>('add')
const form = ref<RuleSaveReq>({
  name: '', code: '', category: 'STYLE', severity: 'WARNING',
  patternType: 'AST', enabled: 1
})
const editId = ref<number | null>(null)

async function load() {
  loading.value = true
  try {
    const r = await ruleApi.list(current.value, size.value, category.value || undefined)
    list.value = r.records || []
    total.value = r.total || 0
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function openAdd() {
  dialogMode.value = 'add'
  editId.value = null
  form.value = { name: '', code: '', category: 'STYLE', severity: 'WARNING', patternType: 'AST', enabled: 1 }
  dialogVisible.value = true
}

function openEdit(row: RuleVO) {
  dialogMode.value = 'edit'
  editId.value = row.id
  form.value = {
    name: row.name,
    code: row.code,
    category: row.category,
    severity: row.severity,
    patternType: row.patternType,
    description: row.description,
    suggestionTemplate: row.suggestionTemplate,
    executorBean: row.executorBean,
    enabled: row.enabled
  }
  dialogVisible.value = true
}

async function submit() {
  if (!form.value.name || !form.value.code) {
    ElMessage.warning('请填写名称和编码')
    return
  }
  try {
    if (dialogMode.value === 'add') {
      await ruleApi.add(form.value)
      ElMessage.success('新增成功')
    } else if (editId.value) {
      await ruleApi.update(editId.value, form.value)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    load()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

async function remove(row: RuleVO) {
  try {
    await ElMessageBox.confirm(`确认删除规则 "${row.name}"？`, '提示', { type: 'warning' })
    await ruleApi.remove(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}

async function toggle(row: RuleVO) {
  try {
    await ruleApi.toggle(row.id, row.enabled === 1 ? 0 : 1)
    ElMessage.success('已更新')
    load()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

async function refreshCache() {
  try {
    await ruleApi.refresh()
    ElMessage.success('缓存已刷新')
  } catch (e: any) {
    ElMessage.error(e?.message || '刷新失败')
  }
}

const sevType = (s: string) => ({ CRITICAL: 'danger', ERROR: 'warning', WARNING: '', SUGGESTION: 'info' } as any)[s] || ''
const catType = (c: string) => ({ STYLE: 'primary', DEFECT: 'warning', SECURITY: 'danger' } as any)[c] || ''

onMounted(load)
</script>

<template>
  <div class="page-container">
    <div class="flex items-center justify-between" style="margin-bottom: 16px">
      <h2 style="margin: 0">规则管理</h2>
      <div class="flex gap-12">
        <el-button @click="refreshCache"><el-icon><Refresh /></el-icon> 刷新缓存</el-button>
        <el-button type="primary" @click="openAdd"><el-icon><Plus /></el-icon> 新增规则</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-radio-group v-model="category" @change="load" style="margin-bottom: 12px">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="STYLE">规范</el-radio-button>
        <el-radio-button value="DEFECT">缺陷</el-radio-button>
        <el-radio-button value="SECURITY">安全</el-radio-button>
      </el-radio-group>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="code" label="编码" width="200" />
        <el-table-column label="分类" width="80">
          <template #default="{ row }">
            <el-tag :type="catType(row.category)" size="small">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="严重级别" width="100">
          <template #default="{ row }">
            <el-tag :type="sevType(row.severity)" size="small">{{ row.severity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="内置" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isBuiltin === 1" type="info" size="small">内置</el-tag>
            <span v-else class="text-sub">-</span>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="80">
          <template #default="{ row }">
            <el-switch :model-value="row.enabled === 1" @change="toggle(row as any)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEdit(row as any)"><el-icon><Edit /></el-icon> 编辑</el-button>
            <el-button v-if="row.isBuiltin !== 1" type="danger" link size="small" @click="remove(row as any)">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="current"
        v-model:page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="load" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'add' ? '新增规则' : '编辑规则'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="编码" required>
          <el-input v-model="form.code" placeholder="全大写下划线，如 NAMING_CLASS" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-radio-group v-model="form.category">
            <el-radio value="STYLE">规范</el-radio>
            <el-radio value="DEFECT">缺陷</el-radio>
            <el-radio value="SECURITY">安全</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="严重级别" required>
          <el-radio-group v-model="form.severity">
            <el-radio value="SUGGESTION">建议</el-radio>
            <el-radio value="WARNING">警告</el-radio>
            <el-radio value="ERROR">错误</el-radio>
            <el-radio value="CRITICAL">严重</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="匹配模式">
          <el-radio-group v-model="form.patternType">
            <el-radio value="AST">AST</el-radio>
            <el-radio value="REGEX">REGEX</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="执行器Bean">
          <el-input v-model="form.executorBean" placeholder="如 namingChecker / sqlInjectionScanner" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="建议模板">
          <el-input v-model="form.suggestionTemplate" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
