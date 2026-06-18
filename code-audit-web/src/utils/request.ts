import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import type { ApiResult } from '@/types/api'

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 60_000
})

service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers = config.headers || {}
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (err) => Promise.reject(err)
)

service.interceptors.response.use(
  (resp: AxiosResponse<ApiResult>) => {
    const body = resp.data
    if (!body || typeof body.code === 'undefined') {
      // 非标准响应，直接返回
      return resp
    }
    if (body.code === 200) {
      return resp
    }
    if (body.code === 401 || body.code === 10002) {
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
      router.push({ name: 'login' })
      return Promise.reject(new Error(body.message || '未登录'))
    }
    ElMessage.error(body.message || '请求失败')
    return Promise.reject(new Error(body.message || '请求失败'))
  },
  (err) => {
    if (err.response?.status === 401) {
      ElMessage.error('登录已过期')
      router.push({ name: 'login' })
    } else if (err.response?.status === 403) {
      ElMessage.error('权限不足')
    } else if (err.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请重试')
    } else {
      ElMessage.error(err.message || '网络异常')
    }
    return Promise.reject(err)
  }
)

export function request<T = any>(config: AxiosRequestConfig): Promise<T> {
  return service.request<ApiResult<T>>(config).then((r) => r.data.data)
}

export default service
