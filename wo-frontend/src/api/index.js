import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000,
  transformResponse: [(data) => {
    if (!data || typeof data !== 'string') {
      return data
    }
    try {
      const fixed = data.replace(/:\s*(\d{16,})/g, ':"$1"')
      return JSON.parse(fixed)
    } catch (e) {
      return data
    }
  }]
})

let refreshingPromise = null

const persistLoginData = (data) => {
  localStorage.setItem('token', data.token)
  localStorage.setItem('userId', data.userId)
  localStorage.setItem('username', data.username)
  localStorage.setItem('realName', data.realName || data.username)
  localStorage.setItem('role', data.role || 'USER')
}

const clearAuth = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
  localStorage.removeItem('realName')
  localStorage.removeItem('role')
}

const requestTokenRefresh = async (token) => {
  const response = await axios.post(`${API_BASE_URL}/auth/refresh`, token, {
    headers: { 'Content-Type': 'text/plain' },
    timeout: 30000
  })
  return response.data
}

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  response => response.data,
  async error => {
    const originalRequest = error.config || {}
    const token = localStorage.getItem('token')
    const isAuthEndpoint = /\/auth\/(login|register|refresh)/.test(originalRequest.url || '')

    if (error.response?.status === 401 && token && !originalRequest._retry && !isAuthEndpoint) {
      originalRequest._retry = true
      try {
        if (!refreshingPromise) {
          refreshingPromise = requestTokenRefresh(token).finally(() => {
            refreshingPromise = null
          })
        }
        const refreshRes = await refreshingPromise
        if (refreshRes?.code === 0 && refreshRes.data?.token) {
          persistLoginData(refreshRes.data)
          originalRequest.headers = originalRequest.headers || {}
          originalRequest.headers.Authorization = `Bearer ${refreshRes.data.token}`
          return api(originalRequest)
        }
      } catch (e) {
        // Continue to auth cleanup below.
      }
    }

    if (error.response?.status === 401) {
      clearAuth()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// Auth
export const login = (data) => api.post('/auth/login', data)
export const register = (data) => api.post('/auth/register', data)
export const refreshToken = (token) => api.post('/auth/refresh', token, {
  headers: { 'Content-Type': 'text/plain' }
})

// Users
export const getUser = (id) => api.get(`/users/${id}`)
export const getUserByUsername = (username) => api.get(`/users/by-username/${username}`)
export const getUsers = (params) => api.get('/users', { params })
export const updateUser = (id, data) => api.put(`/users/${id}`, data)
export const deleteUser = (id) => api.delete(`/users/${id}`)

// Work Orders
export const getWorkOrders = (params) => api.get('/workorders', { params })
export const getWorkOrderStatusStats = (params) => api.get('/workorders/stats/status', { params })
export const getWorkOrder = (id) => api.get(`/workorders/${id}`)
export const createWorkOrder = (data) => api.post('/workorders', data)
export const updateStatus = (id, status, comment) => api.put(`/workorders/${id}/status`, { status, comment })
export const assignWorkOrder = (id, data) => api.put(`/workorders/${id}/assign`, data)
export const confirmWorkOrder = (id) => api.put(`/workorders/${id}/confirm`)
export const rejectWorkOrder = (id, reason) => api.put(`/workorders/${id}/reject`, { reason })
export const regenerateSolution = (id) => api.post(`/workorders/${id}/regenerate`)
export const escalateWorkOrder = (id) => api.put(`/workorders/${id}/escalate`)
export const claimWorkOrder = (id) => api.put(`/workorders/${id}/claim`)
export const getDepartmentWorkOrders = (department, params) => api.get(`/workorders/department/${department}`, { params })
export const getComments = (id) => api.get(`/workorders/${id}/comments`)
export const addComment = (id, data) => api.post(`/workorders/${id}/comments`, data)
export const getFlowRecords = (id) => api.get(`/workorders/${id}/flows`)
export const getAttachments = (id) => api.get(`/workorders/${id}/attachments`)
export const addAttachmentMetadata = (id, data) => api.post(`/workorders/${id}/attachments/metadata`, data)
export const uploadAttachment = (id, file) => {
  const formData = new FormData()
  formData.append('file', file)
  return api.post(`/workorders/${id}/attachments`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// AI
export const chat = (data) => api.post('/ai/chat', data)
export const getChatHistory = (sessionId) => api.get(`/ai/chat/${sessionId}/history`)
export const clearChatSession = (sessionId) => api.delete(`/ai/chat/${sessionId}`)
export const analyzeWorkOrder = (data) => api.post('/ai/analyze', data)

// Knowledge
export const searchKnowledge = (data) => api.post('/ai/knowledge/search', data)
export const indexKnowledge = (data) => api.post('/ai/knowledge/index', data)
export const verifyKnowledge = (id) => api.post('/ai/knowledge/verify', null, { params: { id } })
export const likeKnowledge = (id) => api.post('/ai/knowledge/like', null, { params: { id } })

export default api
