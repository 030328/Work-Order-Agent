import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 60000,
  // 解决大数字精度问题：把超过 16 位的数字转为字符串
  transformResponse: [(data) => {
    try {
      // 用正则把大数字转字符串
      const fixed = data.replace(/:\s*(\d{16,})/g, ':"$1"')
      return JSON.parse(fixed)
    } catch (e) {
      return JSON.parse(data)
    }
  }]
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  const userId = localStorage.getItem('userId')
  if (userId) {
    config.headers['X-User-Id'] = userId
  }
  return config
})

api.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// Auth
export const login = (data) => api.post('/auth/login', data)
export const register = (data) => api.post('/auth/register', data)

// Work Orders
export const getWorkOrders = (params) => api.get('/workorders', { params })
export const getWorkOrderStatusStats = (params) => api.get('/workorders/stats/status', { params })
export const getWorkOrder = (id) => api.get(`/workorders/${id}`)
export const createWorkOrder = (data) => api.post('/workorders', data)
export const updateStatus = (id, status, comment) => api.put(`/workorders/${id}/status`, null, { params: { status, comment } })
export const confirmWorkOrder = (id) => api.put(`/workorders/${id}/confirm`)
export const regenerateSolution = (id) => api.post(`/workorders/${id}/regenerate`)
export const escalateWorkOrder = (id) => api.put(`/workorders/${id}/escalate`)
export const claimWorkOrder = (id) => api.put(`/workorders/${id}/claim`)

// AI Chat
export const chat = (data) => api.post('/ai/chat', data)

// Knowledge
export const searchKnowledge = (data) => api.post('/ai/knowledge/search', data)
export const indexKnowledge = (data) => api.post('/ai/knowledge/index', data)

export default api
