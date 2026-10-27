import { Typography } from '@mui/material'
import { Navigate, RouterProvider, createBrowserRouter } from 'react-router'
import { LoginPage } from './auth/LoginPage'
import { RegisterPage } from './auth/RegisterPage'
import { RequireAuth } from './auth/RequireAuth'
import { ExercisesPage } from './catalog/ExercisesPage'
import { AppLayout } from './components/AppLayout'
import { LocationsPage } from './locations/LocationsPage'

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
          { path: '/checkin', element: <Typography>Check-in is coming soon.</Typography> },
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
