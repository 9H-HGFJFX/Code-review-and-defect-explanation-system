<script setup lang="ts">
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const collapsed = ref(false)
const activeMenu = ref(route.path)

const menus = computed(() => {
  const base = [
    { path: '/',      title: '代码审查', icon: 'EditPen' },
    { path: '/history', title: '历史记录', icon: 'Clock' }
  ]
  if (auth.isTeacher) {
    base.push({ path: '/rules', title: '规则管理', icon: 'SetUp' })
    base.push({ path: '/stats', title: '数据统计', icon: 'DataAnalysis' })
  }
  return base
})

async function handleLogout() {
  await auth.logout()
  ElMessage.success('已退出登录')
  router.replace('/login')
}

onMounted(() => {
  activeMenu.value = route.path
})

// 路由切换时同步高亮
router.afterEach((to) => { activeMenu.value = to.path })
</script>

<template>
  <el-container class="layout">
    <el-aside :width="collapsed ? '64px' : '220px'" class="aside">
      <div class="logo">
        <span v-if="!collapsed">🛡 Code Insight</span>
        <span v-else>🛡</span>
      </div>
      <el-menu :default-active="activeMenu" router :collapse="collapsed" background-color="#1e3a8a" text-color="#cbd5e1" active-text-color="#fff">
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          <el-icon><component :is="m.icon" /></el-icon>
          <template #title>{{ m.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-button text @click="collapsed = !collapsed">
            <el-icon><component :is="collapsed ? 'Expand' : 'Fold'" /></el-icon>
          </el-button>
          <span class="page-title">{{ (route.meta.title as string) || '' }}</span>
        </div>
        <div class="header-right">
          <el-dropdown>
            <span class="user-info">
              <el-avatar :size="28" style="margin-right: 8px; background: #3b82f6">
                {{ (auth.user?.realName || auth.user?.username || 'U').charAt(0).toUpperCase() }}
              </el-avatar>
              {{ auth.user?.realName || auth.user?.username }}
              <el-tag size="small" :type="auth.user?.role === 'ADMIN' ? 'danger' : auth.user?.role === 'TEACHER' ? 'warning' : ''" style="margin-left: 8px">
                {{ auth.user?.role === 'ADMIN' ? '管理员' : auth.user?.role === 'TEACHER' ? '教师' : '学生' }}
              </el-tag>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout { min-height: 100vh; }
.aside { background: #1e3a8a; transition: width 0.2s; }
.logo { height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; font-weight: 700; font-size: 18px; border-bottom: 1px solid #1e40af; }
.header { background: #fff; border-bottom: 1px solid #e5e7eb; display: flex; justify-content: space-between; align-items: center; padding: 0 16px; }
.header-left { display: flex; align-items: center; gap: 12px; }
.page-title { font-size: 16px; font-weight: 600; }
.user-info { display: flex; align-items: center; cursor: pointer; }
.main { background: var(--bg-page); }
:deep(.el-menu) { border-right: 0; }
:deep(.el-menu-item.is-active) { background: #2563eb !important; }
</style>
