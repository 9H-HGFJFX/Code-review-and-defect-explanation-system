import { request } from '@/utils/request'
import type { PageResp, RuleVO, RuleSaveReq } from '@/types/api'

export const ruleApi = {
  list: (current = 1, size = 20, category?: string) =>
    request<PageResp<RuleVO>>({ url: '/rule/list', method: 'get', params: { current, size, category } }),
  detail: (id: number) =>
    request<RuleVO>({ url: `/rule/${id}`, method: 'get' }),
  add: (data: RuleSaveReq) =>
    request<number>({ url: '/rule/add', method: 'post', data }),
  update: (id: number, data: RuleSaveReq) =>
    request<void>({ url: `/rule/update/${id}`, method: 'put', data }),
  remove: (id: number) =>
    request<void>({ url: `/rule/delete/${id}`, method: 'delete' }),
  toggle: (id: number, enabled: number) =>
    request<void>({ url: `/rule/toggle/${id}`, method: 'put', params: { enabled } }),
  refresh: () => request<void>({ url: '/rule/refresh', method: 'post' })
}
