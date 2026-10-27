import { Alert, Snackbar } from '@mui/material'
import { useCallback, useState, type ReactNode } from 'react'
import { NotifyContext, type Severity } from './NotifyContext'

interface Toast {
  key: number
  message: string
  severity: Severity
}

export function NotifyProvider({ children }: { children: ReactNode }) {
  const [toast, setToast] = useState<Toast | null>(null)

  const notify = useCallback((message: string, severity: Severity = 'success') => {
    setToast({ key: Date.now(), message, severity })
  }, [])

  const close = () => setToast(null)

  return (
    <NotifyContext.Provider value={notify}>
      {children}
      <Snackbar
        key={toast?.key}
        open={toast !== null}
        autoHideDuration={4000}
        onClose={close}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
        sx={{ top: { xs: 72, sm: 80 } }}
      >
        <Alert onClose={close} severity={toast?.severity ?? 'success'} variant="filled" sx={{ width: '100%' }}>
          {toast?.message}
        </Alert>
      </Snackbar>
    </NotifyContext.Provider>
  )
}
