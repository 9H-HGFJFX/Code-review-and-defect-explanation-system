import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/home',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'review',
        name: 'Review',
        component: () => import('@/views/Review.vue'),
        meta: { title: '代码审查' }
      },
      {
        path: 'review/history',
        name: 'ReviewHistory',
        component: () => import('@/views/ReviewHistory.vue'),
        meta: { title: '审查历史' }
      },
      {
        path: 'rules',
        name: 'Rules',
        component: () => import('@/views/Rules.vue'),
        meta: { title: '规则管理', roles: ['TEACHER', 'ADMIN'] }
      },
      {
        path: 'class',
        name: 'Class',
        component: () => import('@/views/Class.vue'),
        meta: { title: '班级管理', roles: ['TEACHER'] }
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/Statistics.vue'),
        meta: { title: '数据统计' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 代码审查系统` : '代码审查系统'
  
  const userStore = useUserStore()
  const token = userStore.token
  
  if (to.meta.requiresAuth !== false && !token) {
    next('/login')
    return
  }
  
  if (token && (to.path === '/login' || to.path === '/register')) {
    next('/')
    return
  }
  
  // 检查角色权限
  if (to.meta.roles && token) {
    const userRole = userStore.userInfo?.role
    if (!to.meta.roles.includes(userRole)) {
      next('/home')
      return
    }
  }
  
  next()
})

export default router
