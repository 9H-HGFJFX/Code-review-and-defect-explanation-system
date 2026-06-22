import request from '@/utils/request'

export function getRuleList(params) {
  return request({
    url: '/rule/list',
    method: 'get',
    params
  })
}

export function getRuleDetail(id) {
  return request({
    url: `/rule/${id}`,
    method: 'get'
  })
}

export function createRule(data) {
  return request({
    url: '/rule/add',
    method: 'post',
    data
  })
}

export function updateRule(id, data) {
  return request({
    url: `/rule/update/${id}`,
    method: 'put',
    data
  })
}

export function toggleRule(id, enabled) {
  return request({
    url: `/rule/toggle/${id}`,
    method: 'put',
    params: { enabled }
  })
}

export function deleteRule(id) {
  return request({
    url: `/rule/delete/${id}`,
    method: 'delete'
  })
}

export function refreshRuleCache() {
  return request({
    url: '/rule/refresh-cache',
    method: 'post'
  })
}

export function getRuleStatistics() {
  return request({
    url: '/rule/statistics',
    method: 'get'
  })
}
