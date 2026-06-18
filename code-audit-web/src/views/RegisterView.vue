<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const auth = useAuthStore()

const form = ref({
  username: '',
  password: '',
  confirm: '',
  role: 'STUDENT',
  email: '',
  realName: ''
})
const loading = ref(false)

async function handleRegister() {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请填写完整')
    return
  }
  if (form.value.password !== form.value.confirm) {
    ElMessage.error('两次密码不一致')
    return
  }
  if (form.value.password.length < 6) {
    ElMessage.error('密码至少 6 位')
    return
  }
  loading.value = true
  try {
    await auth.register({ ...form.value })
    ElMessage.success('注册成功')
    router.push('/')
  } catch (e: any) {
    ElMessage.error(e?.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <el-card class="register-card" shadow="always">
      <h2 style="text-align: center; margin: 0 0 16px 0; color: var(--primary)">注册账号</h2>
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="字母开头，3-50 字符" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="form.confirm" type="password" show-password />
        </el-form-item>
        <el-form-item label="角色">
          <el-radio-group v-model="form.role">
            <el-radio value="STUDENT">学生</el-radio>
            <el-radio value="TEACHER">教师</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-button type="primary" :loading="loading" size="large" style="width: 100%" @click="handleRegister">
          注册
        </el-button>
        <div class="text-sub" style="text-align: center; margin-top: 12px">
          已有账号？<router-link to="/login">去登录</router-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.register-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #dbeafe 0%, #eef2ff 100%);
}
.register-card { width: 460px; }
</style>
