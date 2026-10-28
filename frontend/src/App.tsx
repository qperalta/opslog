import { Navigate, RouterProvider, createBrowserRouter } from 'react-router'
import { LoginPage } from './auth/LoginPage'
import { RegisterPage } from './auth/RegisterPage'
import { RequireAuth } from './auth/RequireAuth'
import { CheckInPage } from './checkin/CheckInPage'
import { ExercisesPage } from './catalog/ExercisesPage'
import { HistoryPage } from './history/HistoryPage'
import { AppLayout } from './components/AppLayout'
import { LocationsPage } from './locations/LocationsPage'
import { PlannerPage } from './planner/PlannerPage'
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
          { path: '/checkin', element: <CheckInPage /> },
          { path: '/today', element: <TodayPage /> },
          { path: '/planner', element: <PlannerPage /> },
          { path: '/history', element: <HistoryPage /> },
          { path: '/exercises', element: <ExercisesPage /> },
          { path: '/locations', element: <LocationsPage /> },
        ],
      },
    ],
  },
  { path: '*', element: <Navigate to="/checkin" replace /> },
])

export default function App() {
  return <RouterProvider router={router} />
}
