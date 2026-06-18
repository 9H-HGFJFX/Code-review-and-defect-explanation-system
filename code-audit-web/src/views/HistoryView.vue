<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { reviewApi } from '@/api/review'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const auth = useAuthStore()
const list = ref<any[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(10)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const api = auth.isTeacher ? reviewApi.allHistory : reviewApi.history
    const resp = await api(current.value, size.value)
    list.value = resp.records || []
    total.value = resp.total || 0
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function view(row: any) {
  router.push({ name: 'review', params: { id: row.id } })
}

const title = computed(() => auth.isTeacher ? '全平台审查记录' : '我的审查历史')

onMounted(load)
</script>

<template>
  <div class="page-container">
    <h2>{{ title }}</h2>
    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="fileName" label="文件名" show-overflow-tooltip />
        <el-table-column prop="lineCount" label="行数" width="100" />
        <el-table-column prop="issueCount" label="问题数" width="100">
          <template #default="{ row }">
            <el-tag :type="row.issueCount > 0 ? 'danger' : 'success'" size="small">{{ row.issueCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'COMPLETED' ? 'success' : row.status === 'FAILED' ? 'danger' : ''" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costMs" label="耗时(ms)" width="120" />
        <el-table-column prop="reviewTime" label="时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="view(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="current"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="load"
        @size-change="load" />
    </el-card>
  </div>
</template>
