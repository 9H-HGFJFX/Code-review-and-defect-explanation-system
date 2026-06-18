import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import type { UserInfo } from '@/types/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('accessToken') || '')
  const refreshToken = ref<string>(localStorage.getItem('refreshToken') || '')
  const user = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isTeacher = computed(() => user.value?.role === 'TEACHER' || isAdmin.value)

  function setLogin(payload: { accessToken: string; refreshToken: string; user: UserInfo }) {
    token.value = payload.accessToken
    refreshToken.value = payload.refreshToken
    user.value = payload.user
    localStorage.setItem('accessToken', payload.accessToken)
    localStorage.setItem('refreshToken', payload.refreshToken)
    localStorage.setItem('user', JSON.stringify(payload.user))
  }

  function restore() {
    const t = localStorage.getItem('accessToken')
    const u = localStorage.getItem('user')
    if (t) token.value = t
    if (u) {
      try { user.value = JSON.parse(u) } catch { /* noop */ }
    }
  }

  async function login(username: string, password: string) {
    const data = await authApi.login({ username, password })
    setLogin(data)
  }

  async function register(payload: any) {
    const data = await authApi.register(payload)
    setLogin(data)
  }

  async function logout() {
    try { await authApi.logout() } catch { /* noop */ }
    token.value = ''
    refreshToken.value = ''
    user.value = null
    localStorage.clear()
  }

  return { token, refreshToken, user, isLoggedIn, isAdmin, isTeacher, restore, login, register, logout }
})
