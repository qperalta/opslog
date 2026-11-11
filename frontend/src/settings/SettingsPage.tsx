import LogoutIcon from '@mui/icons-material/Logout'
import { Box, Button, Card, CardContent, Stack, Typography } from '@mui/material'
import { useAuth } from '../auth/AuthContext'
import { PageHeader } from '../components/PageHeader'
import { LocationsSection } from '../locations/LocationsSection'

/* account basics and the things set up once and rarely changed */
export function SettingsPage() {
  const { user, logout } = useAuth()

  return (
    <>
      <PageHeader
        eyebrow="Setup"
        title="Settings"
        subtitle="Your account and training locations"
      />

      <Typography variant="h6" component="h2" sx={{ mb: 1 }}>Account</Typography>
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
            <Box sx={{ flexGrow: 1, minWidth: 0 }}>
              <Typography sx={{ fontWeight: 600 }}>{user?.displayName}</Typography>
              <Typography variant="body2" color="text.secondary" noWrap>{user?.email}</Typography>
            </Box>
            <Button variant="outlined" startIcon={<LogoutIcon />} onClick={logout} sx={{ flexShrink: 0 }}>
              Sign out
            </Button>
          </Stack>
        </CardContent>
      </Card>

      <LocationsSection />
    </>
  )
}
