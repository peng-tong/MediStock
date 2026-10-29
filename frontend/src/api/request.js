// 统一 Axios 实例：注入 token、解包统一响应体 {code,message,data}、401 自动登出
// 注意：这里不引入 store，避免循环依赖，token 直接读写 localStorage
import axios from 'axios'

export const TOKEN_KEY = 'ms_token'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截：自动携带 JWT
request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截：code===0 直接返回 data；401 清空登录态跳登录页
request.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body.code === 'number') {
      if (body.code === 0) return body.data
      if (body.code === 401 || body.code === 4010) {
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem('ms_user')
        if (!location.pathname.includes('/login')) location.href = '/login'
      }
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  (err) => {
    if (err.response && err.response.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem('ms_user')
      if (!location.pathname.includes('/login')) location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export default request
