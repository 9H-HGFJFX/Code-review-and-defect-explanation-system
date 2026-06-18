import { request } from '@/utils/request'
import type { PageResp, ReviewSummary } from '@/types/api'
import service from '@/utils/request'

export const reportApi = {
  /** 触发浏览器下载 PDF/Word */
  download: async (reviewId: number, type: 'pdf' | 'word' = 'pdf') => {
    const url = `/report/export/${reviewId}?type=${type}`
    const resp = await service.get(url, { responseType: 'blob' })
    const blob = new Blob([resp.data])
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `review-${reviewId}.${type === 'word' ? 'docx' : 'pdf'}`
    link.click()
    URL.revokeObjectURL(link.href)
  }
}

export const statApi = {
  overview: () => request<any>({ url: '/statistic/overview', method: 'get' })
}
