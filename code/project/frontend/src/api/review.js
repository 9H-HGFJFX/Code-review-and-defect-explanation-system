import request from '@/utils/request'

export function submitReview(data) {
  return request({
    url: '/review/submit',
    method: 'post',
    data
  })
}

export function getReviewResult(id) {
  return request({
    url: `/review/${id}`,
    method: 'get'
  })
}

export function getReviewList(params) {
  return request({
    url: '/review/list',
    method: 'get',
    params
  })
}

export function pollAsyncResult(taskId) {
  return request({
    url: `/review/async/result/${taskId}`,
    method: 'get'
  })
}
