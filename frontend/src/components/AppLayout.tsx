import LogoutIcon from '@mui/icons-material/Logout'
import { AppBar, BottomNavigation, BottomNavigationAction, Box, Container, IconButton, Paper, Toolbar, Typography } from '@mui/material'
import { Outlet, useLocation, useNavigate } from 'react-router'
import emblem from '../assets/emblem.png'
import { useAuth } from '../auth/AuthContext'
import { NAV_ITEMS } from './navItems'

/* phone-width frame: top bar, page content, bottom navigation */
export function AppLayout() {
  const { user, logout } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
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
            <Typography variant="body2" sx={{ mr: 1 }}>
              {user?.displayName}
            </Typography>
            <IconButton color="inherit" onClick={logout} aria-label="Sign out">
              <LogoutIcon />
            </IconButton>
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
