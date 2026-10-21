import axios from 'axios'
import { MessagePlugin } from 'tdesign-vue-next'

export const TOKEN_KEY = 'cs_token'
export const ROLE_KEY = 'cs_role'
export const NAME_KEY = 'cs_name'

const instance = axios.create({
  baseURL: '/api',
  timeout: 15000
})

instance.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use(
  (resp) => {
    if (resp.config.responseType === 'blob') {
      return resp
    }
    const body = resp.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      MessagePlugin.error(body.message || '操作失败')
      return Promise.reject(new Error(body.message || '操作失败'))
    }
    return body?.data
  },
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(ROLE_KEY)
      localStorage.removeItem(NAME_KEY)
      if (!window.location.pathname.startsWith('/login')) {
        MessagePlugin.error('登录状态已失效，请重新登录')
        window.location.href = '/login'
      }
    } else {
      MessagePlugin.error(error?.response?.data?.message || '网络异常，请稍后再试')
    }
    return Promise.reject(error)
  }
)

export const http = {
  get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
    return instance.get(url, { params }) as unknown as Promise<T>
  },
  post<T>(url: string, data?: unknown, headers?: Record<string, string>): Promise<T> {
    return instance.post(url, data, { headers }) as unknown as Promise<T>
  },
  put<T>(url: string, data?: unknown): Promise<T> {
    return instance.put(url, data) as unknown as Promise<T>
  },
  del<T>(url: string): Promise<T> {
    return instance.delete(url) as unknown as Promise<T>
  },
  download(url: string, params?: Record<string, unknown>) {
    return instance.get(url, { params, responseType: 'blob' })
  }
}
