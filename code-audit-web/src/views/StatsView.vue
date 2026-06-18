<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import StatsChart from '@/components/StatsChart.vue'
import { statApi } from '@/api/report'

const overview = ref<any>(null)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    overview.value = await statApi.overview()
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page-container" v-loading="loading">
    <h2>数据统计看板</h2>

    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="6">
        <el-card shadow="never">
          <div class="text-sub">总审查数</div>
          <div class="big-num">{{ overview?.totalReviews || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="text-sub">总问题数</div>
          <div class="big-num" style="color: #dc2626">{{ overview?.totalIssues || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="text-sub">安全问题数</div>
          <div class="big-num" style="color: #ea580c">{{ overview?.categoryDistribution?.SECURITY || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="text-sub">严重漏洞数</div>
          <div class="big-num" style="color: #dc2626">{{ overview?.severityDistribution?.CRITICAL || 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <StatsChart v-if="overview" :overview="overview" />

    <el-card shadow="never" style="margin-top: 16px">
      <template #header><b>高频问题 Top 10</b></template>
      <el-table :data="overview?.topIssues || []">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="key" label="问题类型" />
        <el-table-column prop="count" label="出现次数" width="120" sortable />
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.big-num { font-size: 32px; font-weight: 700; color: var(--primary); margin-top: 8px; }
</style>
