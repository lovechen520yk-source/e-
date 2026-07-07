import request from './request'

export interface HealthAdviceRequest {
  totalHours: number
  dailyAvg: number
  maxDailyHours: number
  workDays: number
  overtimeDays: number
  userName: string
  yearMonth: string
}

export function getHealthAdvice(data: HealthAdviceRequest) {
  return request.post<{ advice: string }>('/health/advice', data)
}
