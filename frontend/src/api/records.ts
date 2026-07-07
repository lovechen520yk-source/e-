import request from './request'

export interface WorkRecord {
  id?: number
  workDate: string
  company: string
  department: string
  position: string
  content: string
  hours: number
  project: string
  remark?: string
  isRest?: boolean
  createTime?: string
  updateTime?: string
}

export interface RecordQueryParams {
  yearMonth?: string
}

export function addRecord(data: WorkRecord) {
  return request.post('/records', data)
}

export function getRecords(params?: RecordQueryParams) {
  return request.get('/records', { params })
}

export function getRecord(id: number) {
  return request.get(`/records/${id}`)
}

export function updateRecord(id: number, data: WorkRecord) {
  return request.put(`/records/${id}`, data)
}

export function deleteRecord(id: number) {
  return request.delete(`/records/${id}`)
}

export function batchDeleteRecords(ids: number[]) {
  return request.post('/records/batch-delete', { ids })
}
