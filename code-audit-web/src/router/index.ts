import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

const routes = [
  { path: '/', name: 'home', component: () => import('@/views/HomeView.vue'), meta: { title: '代码审查' } },
  { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { title: '登录', noAuth: true } },
  { path: '/register', name: 'register', component: () => import('@/views/RegisterView.vue'), meta: { title: '注册', noAuth: true } },
  { path: '/review/:id', name: 'review', component: () => import('@/views/ReviewView.vue'), meta: { title: '审查详情' } },
  { path: '/history', name: 'history', component: () => import('@/views/HistoryView.vue'), meta: { title: '历史记录' } },
  { path: '/rules', name: 'rules', component: () => import('@/views/RulesView.vue'), meta: { title: '规则管理', roles: ['TEACHER', 'ADMIN'] } },
  { path: '/stats', name: 'stats', component: () => import('@/views/StatsView.vue'), meta: { title: '数据统计', roles: ['TEACHER', 'ADMIN'] } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('accessToken')
  if (to.meta.noAuth) {
    next()
  } else if (!token) {
    ElMessage.warning('请先登录')
    next({ name: 'login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

router.afterEach((to) => {
  if (to.meta.title) document.title = `${to.meta.title} - 代码审查系统`
})

export default router
