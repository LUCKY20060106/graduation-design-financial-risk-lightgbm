import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建 axios 实例
const service = axios.create({
  // 开发环境下使用 /api 前缀触发 Vite 代理，生产环境下使用相对路径
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 60000 // 请求超时时间设定为 60s，以适配大模型响应时间
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    return config
  },
  error => {
    console.log(error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    return res
  },
  error => {
    console.log('err' + error)
    ElMessage({
      message: error.message || '请求后端服务失败，请检查服务是否启动',
      type: 'error',
      duration: 5 * 1000
    })
    return Promise.reject(error)
  }
)

export default service
