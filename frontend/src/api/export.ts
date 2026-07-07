import request from './request'

export function exportExcel(params: { yearMonth: string }) {
  return request.get('/export/excel', {
    params,
    responseType: 'blob'
  })
}

export function downloadBackup() {
  return request.get('/backup/download', {
    responseType: 'blob'
  })
}

export function importBackup(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/backup/import', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
