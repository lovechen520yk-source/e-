/**
 * 简单的内存缓存工具，用于缓存不常变化的 API 响应
 * 默认 TTL 5 分钟
 */

interface CacheEntry<T> {
  data: T
  expiry: number
}

const cache = new Map<string, CacheEntry<unknown>>()

/**
 * 获取缓存数据
 */
export function getCache<T>(key: string): T | null {
  const entry = cache.get(key) as CacheEntry<T> | undefined
  if (!entry) return null
  if (Date.now() > entry.expiry) {
    cache.delete(key)
    return null
  }
  return entry.data
}

/**
 * 设置缓存数据，默认 5 分钟过期
 */
export function setCache<T>(key: string, data: T, ttlMs = 5 * 60 * 1000): void {
  cache.set(key, { data, expiry: Date.now() + ttlMs })
}

/**
 * 清除指定前缀的缓存
 */
export function clearCache(prefix?: string): void {
  if (!prefix) {
    cache.clear()
    return
  }
  const keys = Array.from(cache.keys()).filter(k => k.startsWith(prefix))
  keys.forEach(k => cache.delete(k))
}

/**
 * 带缓存的请求包装器
 * 如果缓存中有数据且未过期，直接返回；否则调用 fetchFn 获取并缓存
 */
export async function withCache<T>(
  key: string,
  fetchFn: () => Promise<T>,
  ttlMs = 5 * 60 * 1000
): Promise<T> {
  const cached = getCache<T>(key)
  if (cached !== null) return cached
  const data = await fetchFn()
  setCache(key, data, ttlMs)
  return data
}
