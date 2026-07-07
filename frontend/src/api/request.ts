import axios from 'axios'
import type { AxiosRequestConfig } from 'axios'
import { showToast } from 'vant'

const _request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
_request.interceptors.request.use(
  (config) => {
    // 从 localStorage 获取 token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
_request.interceptors.response.use(
  (response) => {
    // 如果是 blob 响应（如导出 Excel），直接返回原始数据
    if (response.config.responseType === 'blob') {
      return response.data
    }
    const res = response.data
    if (res.code !== 200) {
      showToast(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res.data
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      switch (status) {
        case 401:
          showToast('未登录或登录已过期')
          localStorage.removeItem('token')
          localStorage.removeItem('username')
          // 避免循环跳转
          if (!window.location.hash.includes('/login')) {
            window.location.hash = '#/login'
          }
          break
        case 403:
          showToast('没有权限访问')
          break
        case 404:
          showToast('请求的资源不存在')
          break
        case 500:
          showToast('服务器错误')
          break
        default:
          showToast(data?.msg || '网络错误')
      }
    } else if (error.code === 'ECONNABORTED') {
      showToast('请求超时')
    } else {
      showToast('网络连接失败')
    }
    return Promise.reject(error)
  }
)

// === GET 请求内存缓存（减少网络往返延迟）===
// 缓存命中时立即返回旧数据，同时后台静默刷新（stale-while-revalidate）
// 写操作（POST/PUT/DELETE）自动清除全部缓存，保证数据一致性
const apiCache = new Map<string, { data: any; time: number }>()
const CACHE_TTL = 30 * 1000 // 30秒

function clearCache() {
  apiCache.clear()
}

// 带缓存的 request 对象
const request = {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const cacheKey = `GET:${url}:${config?.params ? JSON.stringify(config.params) : ''}`
    const cached = apiCache.get(cacheKey)

    // 缓存命中：先返回缓存数据（0延迟），后台静默刷新
    if (cached && Date.now() - cached.time < CACHE_TTL) {
      _request.get(url, config).then(data => {
        apiCache.set(cacheKey, { data, time: Date.now() })
      }).catch(() => {})
      return Promise.resolve(cached.data as T)
    }

    // 缓存未命中：走网络，结果存入缓存
    return _request.get(url, config).then((data: any) => {
      apiCache.set(cacheKey, { data, time: Date.now() })
      return data as T
    })
  },
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    clearCache()
    return _request.post(url, data, config) as unknown as Promise<T>
  },
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    clearCache()
    return _request.put(url, data, config) as unknown as Promise<T>
  },
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    clearCache()
    return _request.delete(url, config) as unknown as Promise<T>
  }
}

export default request
