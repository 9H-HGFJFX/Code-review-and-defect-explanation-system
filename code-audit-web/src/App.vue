<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()

onMounted(() => {
  // 应用启动时恢复登录态
  auth.restore()
})
</script>

<template>
  <router-view />
</template>
