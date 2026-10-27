import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from './AuthContext'

/* sends signed-out users to login and remembers where they were going */
export function RequireAuth() {
  const { user } = useAuth()
  const location = useLocation()
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <Outlet />
}
