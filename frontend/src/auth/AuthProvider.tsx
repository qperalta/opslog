import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, setUnauthorizedHandler } from '../api/client'
import type { LoginResponse } from '../api/types'
import { AuthContext } from './AuthContext'
import { clearSession, loadSession, saveSession } from './session'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<LoginResponse | null>(loadSession)
  const queryClient = useQueryClient()

  const logout = useCallback(() => {
    clearSession()
    setSession(null)
    // drop cached data so the next user never sees it
    queryClient.clear()
  }, [queryClient])

  const login = useCallback(async (email: string, password: string) => {
    const response = await api<LoginResponse>('/api/auth/login', { method: 'POST', body: { email, password } })
    saveSession(response)
    setSession(response)
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(logout)
    return () => setUnauthorizedHandler(null)
  }, [logout])

  /* signs out when the JWT expires instead of waiting for a 401 */
  useEffect(() => {
    if (!session) return
    const timer = setTimeout(logout, Math.max(Date.parse(session.expiresAt) - Date.now(), 0))
    return () => clearTimeout(timer)
  }, [session, logout])

  const value = useMemo(() => ({ user: session?.user ?? null, login, logout }), [session, login, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
