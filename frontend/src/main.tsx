import { CssBaseline, ThemeProvider, createTheme } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App.tsx'
import { AuthProvider } from './auth/AuthProvider'
import { NotifyProvider } from './components/NotifyProvider'

const theme = createTheme({
  palette: {
    primary: { main: '#0f5e6e' },
    secondary: { main: '#e07a3f' },
    background: { default: '#f4f6f7' },
  },
  shape: { borderRadius: 10 },
  components: {
    // a picked score should be obvious at a glance
    MuiToggleButton: {
      styleOverrides: {
        root: ({ theme }) => ({
          '&.Mui-selected, &.Mui-selected:hover': {
            backgroundColor: theme.palette.primary.main,
            color: theme.palette.primary.contrastText,
          },
        }),
      },
    },
  },
})

const queryClient = new QueryClient({
  // a 4xx will not succeed on retry
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <QueryClientProvider client={queryClient}>
        <NotifyProvider>
          <AuthProvider>
            <App />
          </AuthProvider>
        </NotifyProvider>
      </QueryClientProvider>
    </ThemeProvider>
  </StrictMode>,
)
