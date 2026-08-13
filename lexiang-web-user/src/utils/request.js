import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000
})

const MAX_RETRIES = 1
const retryCount = new Map()

/** 判断请求是否为 admin 端 */
const isAdminRequest = (url) => {
  if (!url) return false
  // 同时兼容 /admin/xxx、/merchant/xxx 以及带 /api 前缀的 /api/admin/xxx、/api/merchant/xxx
  return /^\/(api\/)?(admin|merchant)\//.test(url)
}

/** 请求拦截器 —— 智能注入 Token */
service.interceptors.request.use(
  config => {
    const admin = isAdminRequest(config.url)
    const token = admin
      ? localStorage.getItem('adminToken')
      : localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

/** 响应拦截器 —— 数据解包 + 错误分级处理 */
service.interceptors.response.use(
  response => {
    const { code, message, data } = response.data
    if (code === 200) return data
    ElMessage.error(message || '操作失败')
    return Promise.reject(new Error(message))
  },
  error => {
    const status = error.response?.status
    const msg = error.response?.data?.message
    const url = error.config?.url || ''
    const admin = isAdminRequest(url)

    // GET 请求自动重试 1 次
    if (error.config && error.config.method === 'get') {
      const key = error.config.url
      const tries = retryCount.get(key) || 0
      if (tries < MAX_RETRIES) {
        retryCount.set(key, tries + 1)
        setTimeout(() => retryCount.delete(key), 5000)
        return service.request(error.config)
      }
    }

    switch (status) {
      case 401:
        // 按请求类型隔离清除 token，不清空另一端的登录态
        if (admin) {
          ;['adminToken','adminUserId','adminUserName','adminUserType']
            .forEach(k => localStorage.removeItem(k))
          window.location.href = '/admin/login'
        } else {
          ;['token','userId','userName','userType']
            .forEach(k => localStorage.removeItem(k))
          window.location.href = '/login'
        }
        break
      case 403:
        ElMessage.error('无权限操作')
        break
      case 500:
        ElMessage.error(msg || '服务器繁忙，请稍后重试')
        break
      default:
        if (!error.response) {
          ElMessage.error('网络连接失败，请检查网络')
        } else {
          ElMessage.error(msg || '请求失败')
        }
    }
    return Promise.reject(error)
  }
)

export default service
