import request from './request'
import { withCache } from '@/utils/cache'

export interface OptionsData {
  companies: string[]
  departments: string[]
  positions: string[]
  projects: string[]
}

export function getOptions() {
  return withCache('meta:options', () => request.get('/meta/options'), 5 * 60 * 1000)
}
