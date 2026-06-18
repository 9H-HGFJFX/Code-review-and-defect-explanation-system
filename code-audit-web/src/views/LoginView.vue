<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const form = ref({ username: '', password: '' })
const loading = ref(false)

async function handleLogin() {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await auth.login(form.value.username, form.value.password)
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (e: any) {
    ElMessage.error(e?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card" shadow="always">
      <div class="logo-text" style="text-align: center; margin-bottom: 16px">
        <h2 style="margin: 0">&lt;/&gt; CodeInsight</h2>
        <div class="text-sub">代码审查与缺陷解释系统</div>
      </div>
      <el-form :model="form" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" size="large" @keyup.enter="handleLogin" />
        </el-form-item>
        <el-button type="primary" :loading="loading" size="large" style="width: 100%" @click="handleLogin">
          登录
        </el-button>
        <div class="text-sub" style="text-align: center; margin-top: 12px">
          没有账号？<router-link to="/register">立即注册</router-link>
        </div>
        <el-divider>默认账号</el-divider>
        <el-descriptions :column="1" size="small" border>
          <el-descriptions-item label="管理员">admin / 123456</el-descriptions-item>
          <el-descriptions-item label="教师">teacher / 123456</el-descriptions-item>
          <el-descriptions-item label="学生">student / 123456</el-descriptions-item>
        </el-descriptions>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #dbeafe 0%, #eef2ff 100%);
}
.login-card { width: 400px; }
.logo-text h2 { color: var(--primary); }
</style>
