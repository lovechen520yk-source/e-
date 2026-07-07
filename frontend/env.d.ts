/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

interface ResponseResult<T = any> {
  code: number
  message: string
  data: T
}

interface PageResult<T = any> {
  total: number
  page: number
  pageSize: number
  records: T[]
}
