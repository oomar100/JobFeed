import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import Layout from './components/layout/Layout'
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'
import DashboardPage from './pages/DashboardPage'
import TaskDetailPage from './pages/TaskDetailPage'
import JobsPage from './pages/JobsPage'
import AppliedPage from './pages/AppliedPage'
import { LoadingOverlay } from '@mantine/core'

function ProtectedRoute({ children }) {
  const { user, loading } = useAuth()
  
  if (loading) {
    return <LoadingOverlay visible />
  }
  
  if (!user) {
    return <Navigate to="/login" replace />
  }
  
  return children
}

function PublicRoute({ children }) {
  const { user, loading } = useAuth()
  
  if (loading) {
    return <LoadingOverlay visible />
  }
  
  if (user) {
    return <Navigate to="/" replace />
  }
  
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<PublicRoute><LoginPage /></PublicRoute>} />
      <Route path="/signup" element={<PublicRoute><SignupPage /></PublicRoute>} />
      
      <Route path="/" element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route index element={<DashboardPage />} />
        <Route path="tasks/:taskId" element={<TaskDetailPage />} />
        <Route path="jobs" element={<JobsPage />} />
        <Route path="applied" element={<AppliedPage />} />
      </Route>
    </Routes>
  )
}
