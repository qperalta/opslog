import { Navigate, RouterProvider, createBrowserRouter } from 'react-router'
import { LoginPage } from './auth/LoginPage'
import { RegisterPage } from './auth/RegisterPage'
import { RequireAuth } from './auth/RequireAuth'
import { ExercisesPage } from './catalog/ExercisesPage'
import { AppLayout } from './components/AppLayout'
import { PlannerPage } from './planner/PlannerPage'
import { SettingsPage } from './settings/SettingsPage'
import { StatsPage } from './stats/StatsPage'
import { TodayPage } from './today/TodayPage'

// paths here must also be listed in SpaController on the server
const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  {
    element: <RequireAuth />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { path: '/today', element: <TodayPage /> },
          { path: '/planner', element: <PlannerPage /> },
          { path: '/stats', element: <StatsPage /> },
          { path: '/exercises', element: <ExercisesPage /> },
          { path: '/settings', element: <SettingsPage /> },
        ],
      },
    ],
  },
  { path: '*', element: <Navigate to="/today" replace /> },
])

export default function App() {
  return <RouterProvider router={router} />
}
