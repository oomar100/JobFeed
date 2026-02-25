import React from 'react'
import ReactDOM from 'react-dom/client'
import { MantineProvider } from '@mantine/core'
import { Notifications } from '@mantine/notifications'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import { AuthProvider } from './context/AuthContext'
import { ThemeProvider, useThemeContext } from './context/ThemeContext'
import '@mantine/core/styles.css'
import '@mantine/notifications/styles.css'
import './index.css'

function AppWithTheme() {
  const { colorScheme } = useThemeContext()
  
  return (
    <MantineProvider defaultColorScheme={colorScheme} forceColorScheme={colorScheme}>
      <Notifications position="top-right" />
      <BrowserRouter>
        <AuthProvider>
          <App />
        </AuthProvider>
      </BrowserRouter>
    </MantineProvider>
  )
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <ThemeProvider>
      <AppWithTheme />
    </ThemeProvider>
  </React.StrictMode>
)
