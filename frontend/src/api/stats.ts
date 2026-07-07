import request from './request'

export interface GroupStatsParams {
  groupBy?: 'project' | 'company' | 'department' | 'position'
  yearMonth?: string
}

export interface GroupStatsItem {
  name: string
  totalHours: number
  totalDays: number
  recordCount: number
}

export function getGroupStats(params: GroupStatsParams) {
  return request.get('/stats/group', { params })
}

export function getSalary(params: { yearMonth: string }) {
  return request.get('/stats/salary', { params })
}
