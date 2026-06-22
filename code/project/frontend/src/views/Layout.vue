<template>
  <el-container class="layout-container">
    <el-aside width="200px" class="sidebar">
      <div class="logo">
        <h2>代码审查</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        class="sidebar-menu"
      >
        <el-menu-item index="/home">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
        
        <el-menu-item index="/review">
          <el-icon><Document /></el-icon>
          <span>代码审查</span>
        </el-menu-item>
        
        <el-menu-item index="/review/history">
          <el-icon><Clock /></el-icon>
          <span>审查历史</span>
        </el-menu-item>
        
        <el-menu-item 
          v-if="isTeacher || isAdmin" 
          index="/rules"
        >
          <el-icon><Setting /></el-icon>
          <span>规则管理</span>
        </el-menu-item>
        
        <el-menu-item 
          v-if="isTeacher" 
          index="/class"
        >
          <el-icon><UserFilled /></el-icon>
          <span>班级管理</span>
        </el-menu-item>
        
        <el-menu-item index="/statistics">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据统计</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <h3>{{ pageTitle }}</h3>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><User /></el-icon>
              <span>{{ username }}</span>
              <el-tag size="small" :type="roleTagType">{{ roleName }}</el-tag>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

const pageTitle = computed(() => route.meta.title || '代码审查系统')

const username = computed(() => userStore.userInfo?.username || 'User')

const isTeacher = computed(() => userStore.isTeacher)
const isAdmin = computed(() => userStore.isAdmin)

const roleName = computed(() => {
  const role = userStore.userInfo?.role
  switch (role) {
    case 'ADMIN': return '管理员'
    case 'TEACHER': return '教师'
    case 'STUDENT': return '学生'
    default: return role
  }
})

const roleTagType = computed(() => {
  const role = userStore.userInfo?.role
  switch (role) {
    case 'ADMIN': return 'danger'
    case 'TEACHER': return 'warning'
    case 'STUDENT': return 'success'
    default: return 'info'
  }
})

const handleCommand = async (command) => {
  if (command === 'logout') {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
.layout-container {
  width: 100%;
  height: 100vh;
}

.sidebar {
  background: #304156;
  
  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #263445;
    
    h2 {
      color: #fff;
      font-size: 18px;
    }
  }
  
  .sidebar-menu {
    border-right: none;
    background: #304156;
    
    :deep(.el-menu-item) {
      color: #bfcbd9;
      
      &:hover, &.is-active {
        background: #263445;
        color: #409eff;
      }
    }
  }
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  
  .header-left h3 {
    color: #333;
    font-size: 18px;
  }
  
  .user-info {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
  }
}

.main-content {
  background: #f0f2f5;
  padding: 20px;
}
</style>
