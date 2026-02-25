import api from './api'

export const taskService = {
  getAll: async () => {
    const response = await api.get('/api/v1/tasks')
    return response.data.content
  },

  getById: async (taskId) => {
    const response = await api.get(`/api/v1/tasks/${taskId}`)
    return response.data
  },

  create: async (taskData) => {
    const response = await api.post('/api/v1/tasks', taskData)
    return response.data
  },

  update: async (taskId, taskData) => {
    const response = await api.put(`/api/v1/tasks/${taskId}`, taskData)
    return response.data
  },

  delete: async (taskId) => {
    await api.delete(`/api/v1/tasks/${taskId}`)
  },

  run: async (taskId) => {
    const response = await api.post(`/api/v1/tasks/${taskId}/run`)
    return response.data
  },

  pause: async (taskId) => {
    const response = await api.post(`/api/v1/tasks/${taskId}/pause`)
    return response.data
  },

  resume: async (taskId) => {
    const response = await api.post(`/api/v1/tasks/${taskId}/resume`)
    return response.data
  },
}
