import { createContext, useContext, useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { authService } from '../services/authService'
import { notifications } from '@mantine/notifications'

const AuthContext = createContext()

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    checkAuth()
  }, [])

  const checkAuth = async () => {
    const token = localStorage.getItem('accessToken')
    if (!token) {
      setLoading(false)
      return
    }

    try {
      const userData = await authService.getMe()
      setUser(userData)
    } catch (error) {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
    } finally {
      setLoading(false)
    }
  }

  const login = async (email, password) => {
    try {
      const response = await authService.login(email, password)
      localStorage.setItem('accessToken', response.access_token)
      localStorage.setItem('refreshToken', response.refresh_token)
      
      const userData = await authService.getMe()
      setUser(userData)
      
      notifications.show({
        title: 'Welcome back!',
        message: 'Successfully logged in',
        color: 'green',
      })
      
      navigate('/')
    } catch (error) {
      notifications.show({
        title: 'Login failed',
        message: error.response?.data?.error || 'Invalid credentials',
        color: 'red',
      })
      throw error
    }
  }

  const signup = async (email, password, firstName, lastName) => {
    try {
      const response = await authService.signup(email, password, firstName, lastName)
      localStorage.setItem('accessToken', response.access_token)
      localStorage.setItem('refreshToken', response.refresh_token)
      
      const userData = await authService.getMe()
      setUser(userData)
      
      notifications.show({
        title: 'Welcome!',
        message: 'Account created successfully',
        color: 'green',
      })
      
      navigate('/')
    } catch (error) {
      notifications.show({
        title: 'Signup failed',
        message: error.response?.data?.error || 'Could not create account',
        color: 'red',
      })
      throw error
    }
  }

  const logout = () => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    setUser(null)
    navigate('/login')
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, signup, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}
