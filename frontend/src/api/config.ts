import request from './request'

export interface UserConfig {
  id?: number
  username?: string
  name?: string
  password?: string
  avatar?: string
  company: string
  department: string
  position: string
  project?: string
  content?: string
  salaryMode: 'HOURLY' | 'FIXED'
  hourlyRate?: number
  fixedMonthlySalary?: number
  overtimeHourlyRate?: number
  allowance?: number
  performanceBonus?: number
  deduction?: number
}

/** 获取用户配置 */
export function getConfig() {
  return request.get('/user/config')
}

/** 保存用户配置 */
export function saveConfig(data: UserConfig) {
  return request.post('/user/config', data)
}

/** 修改密码 */
export function changePassword(data: {
  originalPassword: string
  newPassword: string
}) {
  return request.post('/user/change-password', data)
}
