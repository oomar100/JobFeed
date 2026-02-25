import api from './api'

export const authService = {
  login: async (email, password) => {
    const response = await api.post('/auth/login', { email, password })
    return response.data
  },

  signup: async (email, password, firstName, lastName) => {
    const response = await api.post('/auth/signup', {
      email,
      password,
      firstName,
      lastName,
    })
    return response.data
  },

  refresh: async (refreshToken) => {
    const response = await api.post('/auth/refresh', { refreshToken })
    return response.data
  },

  getMe: async () => {
    const response = await api.get('/auth/me')
    return response.data
  },
}
