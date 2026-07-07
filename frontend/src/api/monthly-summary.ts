import request from './request'

export interface MonthlySummary {
  id?: number
  userId?: number
  yearMonth: string
  year: number
  totalHours: number
  totalSalary: number
  recordCount: number
  createdAt?: string
}

/** 生成指定月份的总结 */
export function generateSummary(yearMonth: string) {
  return request.post('/monthly-summary/generate', null, { params: { yearMonth } })
}

/** 尝试自动生成上个月的总结（每月1日调用） */
export function tryGenerateLastMonth() {
  return request.post('/monthly-summary/try-generate-last-month')
}

/** 获取某年的所有月度总结 */
export function getSummariesByYear(year: number): Promise<MonthlySummary[]> {
  return request.get(`/monthly-summary/year/${year}`)
}

/** 获取用户所有有数据的年份 */
export function getSummaryYears(): Promise<number[]> {
  return request.get('/monthly-summary/years')
}

/** 导出某年月度总结为Excel */
export function exportYearSummary(year: number) {
  const token = localStorage.getItem('token')
  const baseUrl = '/api/monthly-summary/export/' + year
  // 创建隐藏的a标签下载
  const a = document.createElement('a')
  a.href = import.meta.env.VITE_API_BASE_URL
    ? `${import.meta.env.VITE_API_BASE_URL}${baseUrl}`
    : baseUrl
  a.setAttribute('download', `月度总结_${year}.xlsx`)
  // 需要手动添加token
  const xhr = new XMLHttpRequest()
  xhr.open('GET', a.href)
  xhr.setRequestHeader('Authorization', `Bearer ${token}`)
  xhr.responseType = 'blob'
  xhr.onload = () => {
    const url = URL.createObjectURL(xhr.response)
    const link = document.createElement('a')
    link.href = url
    link.download = `月度总结_${year}.xlsx`
    link.click()
    URL.revokeObjectURL(url)
  }
  xhr.send()
}
