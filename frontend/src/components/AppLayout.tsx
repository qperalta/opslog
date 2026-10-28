import PlaceIcon from '@mui/icons-material/Place'
import {
  AppBar, BottomNavigation, BottomNavigationAction, Box, Chip, Container, Paper, Toolbar, Typography,
} from '@mui/material'
import { Outlet, useLocation, useNavigate } from 'react-router'
import emblem from '../assets/emblem.png'
import { useAuth } from '../auth/AuthContext'
import { todayLocal } from '../checkin/dates'
import { useCheckIn } from '../checkin/queries'
import { NAV_ITEMS } from './navItems'

/* phone-width frame: top bar, page content, bottom navigation */
export function AppLayout() {
  const { user } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  // shares the Today screen's query, so it updates on check-in
  const checkIn = useCheckIn(todayLocal())
  const place = checkIn.data?.location?.name
  const current = NAV_ITEMS.find((item) => location.pathname.startsWith(item.path))?.path ?? false

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default', pb: 9 }}>
      <AppBar position="sticky">
        <Container maxWidth="sm" disableGutters>
          <Toolbar>
            <Box component="img" src={emblem} alt="" sx={{ width: 28, height: 28, mr: 1.5 }} />
            <Typography variant="h6" sx={{ flexGrow: 1 }}>
              OpsLog
            </Typography>
            {place && (
              <Chip size="small" icon={<PlaceIcon />} label={place} aria-label={`Training at ${place}`}
                    sx={{ mr: 1, maxWidth: 150, color: 'inherit', bgcolor: 'rgba(255,255,255,0.15)', '& .MuiChip-icon': { color: 'inherit' } }} />
            )}
            <Typography variant="body2" sx={{ mr: 1, display: { xs: 'none', sm: 'block' } }}>
              {user?.displayName}
            </Typography>
          </Toolbar>
        </Container>
      </AppBar>
      <Container maxWidth="sm" sx={{ py: 2 }}>
        <Outlet />
      </Container>
      <Paper elevation={3} sx={{ position: 'fixed', bottom: 0, left: 0, right: 0 }}>
        <Container maxWidth="sm" disableGutters>
          <BottomNavigation showLabels value={current} onChange={(_, path: string) => navigate(path)}>
            {NAV_ITEMS.map(({ path, label, Icon }) => (
              <BottomNavigationAction key={path} value={path} label={label} icon={<Icon />} sx={{ minWidth: 0, px: 0.5 }} />
            ))}
          </BottomNavigation>
        </Container>
      </Paper>
    </Box>
  )
}
