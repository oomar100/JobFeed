import api from './api'

export const jobService = {
  getAll: async (params = {}) => {
    const response = await api.get('/api/v1/jobs', { params })
    return response.data
  },

  getByTask: async (taskId) => {
    const response = await api.get(`/api/v1/jobs/task/${taskId}`)
    return response.data
  },

  getApplied: async (params = {}) => {
    const response = await api.get('/api/v1/jobs', {
      params: { ...params, bucket: 'APPLIED' },
    })
    return response.data
  },

  getById: async (jobId) => {
    const response = await api.get(`/api/v1/jobs/${jobId}`)
    return response.data
  },

  updateBucket: async (jobId, bucket) => {
    const response = await api.patch(`/api/v1/jobs/${jobId}/bucket`, { bucket })
    return response.data
  },

  markAsApplied: async (jobId) => {
    return jobService.updateBucket(jobId, 'APPLIED')
  },

  removeFromApplied: async (jobId) => {
    return jobService.updateBucket(jobId, 'NONE')
  },

  delete: async (jobId) => {
    await api.delete(`/api/v1/jobs/${jobId}`)
  },

  deleteByTask: async (taskId) => {
    await api.delete(`/api/v1/jobs/task/${taskId}`)
  },

  getCount: async () => {
    const response = await api.get('/api/v1/jobs/count')
    return response.data
  },
}
