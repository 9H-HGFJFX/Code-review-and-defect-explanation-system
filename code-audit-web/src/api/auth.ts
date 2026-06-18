import { request } from '@/utils/request'
import type { LoginResp, UserInfo } from '@/types/api'

export const authApi = {
  login: (data: { username: string; password: string }) =>
    request<LoginResp>({ url: '/auth/login', method: 'post', data }),
  register: (data: any) =>
    request<LoginResp>({ url: '/auth/register', method: 'post', data }),
  refresh: (refreshToken: string) =>
    request<LoginResp>({ url: '/auth/refresh', method: 'post', params: { refreshToken } }),
  logout: () => request<void>({ url: '/auth/logout', method: 'post' }),
  me: () => request<UserInfo>({ url: '/auth/me', method: 'get' })
}
