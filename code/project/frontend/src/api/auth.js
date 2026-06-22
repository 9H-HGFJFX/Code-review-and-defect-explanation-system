import request from '@/utils/request'

export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function register(data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

export function logout(token) {
  return request({
    url: '/auth/logout',
    method: 'post',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  })
}

export function refreshToken(refreshToken) {
  return request({
    url: '/auth/refresh',
    method: 'post',
    headers: {
      'Authorization': `Bearer ${refreshToken}`
    }
  })
}
