import request from '@/utils/request'

export function getOverviewStatistics() {
  return request({
    url: '/statistic/overview',
    method: 'get'
  })
}

export function getUserOverviewStatistics() {
  return request({
    url: '/statistic/user/overview',
    method: 'get'
  })
}

export function getClassOverviewStatistics(classId) {
  return request({
    url: '/statistic/class/overview',
    method: 'get',
    params: { classId }
  })
}

export function getReviewTrend(days) {
  return request({
    url: '/statistic/review/trend',
    method: 'get',
    params: { days }
  })
}

export function getIssueTrend(days) {
  return request({
    url: '/statistic/issue/trend',
    method: 'get',
    params: { days }
  })
}

export function getIssueDistribution() {
  return request({
    url: '/statistic/issue/distribution',
    method: 'get'
  })
}

export function getIssueDistributionByCategory() {
  return request({
    url: '/statistic/issue/category',
    method: 'get'
  })
}
