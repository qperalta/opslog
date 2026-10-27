import { createContext, useContext } from 'react'

export type Severity = 'success' | 'error' | 'info'

export const NotifyContext = createContext<((message: string, severity?: Severity) => void) | null>(null)

/* shows a confirmation or error toast */
export function useNotify() {
  const notify = useContext(NotifyContext)
  if (!notify) throw new Error('useNotify must be used inside NotifyProvider')
  return notify
}
