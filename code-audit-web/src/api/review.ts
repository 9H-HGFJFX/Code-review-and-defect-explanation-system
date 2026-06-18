import { request } from '@/utils/request'
import type { PageResp, ReviewResultVO, ReviewSubmitReq, ReviewSummary } from '@/types/api'

export const reviewApi = {
  submit: (data: ReviewSubmitReq) =>
    request<ReviewResultVO>({ url: '/review/submit', method: 'post', data }),
  detail: (id: number) =>
    request<ReviewResultVO>({ url: `/review/${id}`, method: 'get' }),
  history: (current = 1, size = 10) =>
    request<PageResp<ReviewSummary>>({ url: '/review/list', method: 'get', params: { current, size } }),
  allHistory: (current = 1, size = 10) =>
    request<PageResp<ReviewSummary>>({ url: '/review/list-all', method: 'get', params: { current, size } })
}
