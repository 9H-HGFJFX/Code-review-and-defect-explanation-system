<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { reviewApi } from '@/api/review'
import ReportViewer from '@/components/ReportViewer.vue'

const route = useRoute()
const router = useRouter()
const result = ref<any>(null)
const loading = ref(false)

async function load() {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  try {
    result.value = await reviewApi.detail(id)
  } catch (e) {
    /* noop */
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => route.params.id, load)
</script>

<template>
  <div class="page-container">
    <el-page-header @back="router.push('/')" style="margin-bottom: 16px">
      <template #content>
        <span class="text-sub">返回首页</span>
      </template>
    </el-page-header>
    <div v-loading="loading">
      <ReportViewer v-if="result" :result="result" />
    </div>
  </div>
</template>
