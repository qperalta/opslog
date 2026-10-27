import type { LoginResponse } from '../api/types'

// the JWT lives in localStorage so a refresh keeps the user signed in until it expires
const KEY = 'opslog.session'

export function loadSession(): LoginResponse | null {
  try {
    const raw = localStorage.getItem(KEY)
    if (!raw) return null
    const session = JSON.parse(raw) as LoginResponse
    if (Date.parse(session.expiresAt) <= Date.now()) {
      localStorage.removeItem(KEY)
      return null
    }
    return session
  } catch {
    return null
  }
}

export function saveSession(session: LoginResponse) {
  localStorage.setItem(KEY, JSON.stringify(session))
}

export function clearSession() {
  localStorage.removeItem(KEY)
}
