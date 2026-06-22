import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { login as apiLogin, register as apiRegister, logout as apiLogout, refreshToken as apiRefresh } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  // State
  const token = ref(getToken() || '')
  const userInfo = ref(null)
  
  // Getters
  const isLoggedIn = computed(() => !!token.value)
  const userRole = computed(() => userInfo.value?.role)
  const isAdmin = computed(() => userRole.value === 'ADMIN')
  const isTeacher = computed(() => userRole.value === 'TEACHER')
  const isStudent = computed(() => userRole.value === 'STUDENT')
  
  // Actions
  async function login(loginForm) {
    try {
      const res = await apiLogin(loginForm)
      if (res.code === 200) {
        token.value = res.data.accessToken
        userInfo.value = {
          id: res.data.userId,
          username: res.data.username,
          role: res.data.role
        }
        setToken(res.data.accessToken, res.data.refreshToken)
        return { success: true }
      } else {
        return { success: false, message: res.message }
      }
    } catch (error) {
      return { success: false, message: error.message || '登录失败' }
    }
  }
  
  async function register(registerForm) {
    try {
      const res = await apiRegister(registerForm)
      if (res.code === 200) {
        return { success: true, message: '注册成功' }
      } else {
        return { success: false, message: res.message }
      }
    } catch (error) {
      return { success: false, message: error.message || '注册失败' }
    }
  }
  
  async function logout() {
    try {
      if (token.value) {
        await apiLogout(token.value)
      }
    } catch (error) {
      console.error('登出失败', error)
    } finally {
      token.value = ''
      userInfo.value = null
      removeToken()
    }
  }
  
  async function refreshToken() {
    try {
      const refreshTokenVal = localStorage.getItem('refreshToken')
      if (!refreshTokenVal) {
        throw new Error('No refresh token')
      }
      
      const res = await apiRefresh(refreshTokenVal)
      if (res.code === 200) {
        token.value = res.data.accessToken
        setToken(res.data.accessToken, res.data.refreshToken)
        return true
      }
      return false
    } catch (error) {
      console.error('刷新Token失败', error)
      logout()
      return false
    }
  }
  
  function setUserInfo(info) {
    userInfo.value = info
  }
  
  return {
    token,
    userInfo,
    isLoggedIn,
    userRole,
    isAdmin,
    isTeacher,
    isStudent,
    login,
    register,
    logout,
    refreshToken,
    setUserInfo
  }
})
