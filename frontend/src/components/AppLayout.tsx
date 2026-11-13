import PlaceIcon from '@mui/icons-material/Place'
import {
  AppBar, Avatar, BottomNavigation, BottomNavigationAction, Box, Chip, Container, IconButton, Paper, Toolbar, Tooltip, Typography, alpha
} from '@mui/material'
import { Outlet, useLocation, useNavigate } from 'react-router'
import emblem from '../assets/emblem.png'
import { useAuth } from '../auth/AuthContext'
import { todayLocal } from '../checkin/dates'
import { initialsOf } from './format'
import { useCheckIn } from '../checkin/queries'
import { NAV_ITEMS } from './navItems'
import { useAiCoach } from '../mcp/queries'
import { WakeBar } from './WakeBar'
import { useKeepAwake } from './useKeepAwake'

/* phone-width frame: top bar, page content, bottom navigation */
export function AppLayout() {
  const { user } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  // shares the Today screen's query, so it updates on check-in
  const checkIn = useCheckIn(todayLocal())
  const place = checkIn.data?.location?.name
  const aiActive = useAiCoach().data?.active ?? false
  const current = NAV_ITEMS.find((item) => location.pathname.startsWith(item.path))?.path ?? false

  useKeepAwake()

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default', pb: 11 }}>
      <AppBar position="sticky">
        <Container maxWidth="sm" disableGutters>
          <Toolbar>
            <Box component="img" src={emblem} alt="" sx={{ width: 28, height: 28, mr: 1.5 }} />
            <Box sx={{ flexGrow: 1, minWidth: 0 }}>
              <Typography variant="h6" sx={{ lineHeight: 1.2 }}>
                OpsLog
              </Typography>
              {/* today's location, small, under the title */}
              {place && (
                <Chip size="small" icon={<PlaceIcon />} label={place} aria-label={`Training at ${place}`}
                      sx={{ height: 20, maxWidth: 200, fontSize: '0.7rem', color: 'inherit', bgcolor: 'rgba(255,255,255,0.15)',
                        '& .MuiChip-icon': { color: 'inherit', fontSize: 14 }, '& .MuiChip-label': { px: 0.75 } }} />
              )}
            </Box>
            {/* the member's initials; opens their Stats */}
            <Tooltip title={user?.displayName ?? 'Stats'}>
              <IconButton color="inherit" onClick={() => navigate('/stats')} aria-label="Your stats" sx={{ p: 0.5 }}>
                <Avatar sx={{ width: 34, height: 34, fontSize: '0.875rem', fontWeight: 600, color: 'inherit',
                  bgcolor: 'rgba(255,255,255,0.2)', border: '1.5px solid rgba(255,255,255,0.7)' }}>
                  {initialsOf(user?.displayName)}
                </Avatar>
              </IconButton>
            </Tooltip>
          </Toolbar>
          <WakeBar />
        </Container>
      </AppBar>
      <Container maxWidth="sm" sx={{ py: 2 }}>
        <Outlet />
      </Container>
      <Paper
        elevation={6}
        sx={(theme) => ({
          position: 'fixed', bottom: 12, left: 12, right: 12, mx: 'auto', maxWidth: 560, zIndex: 'appBar',
          borderRadius: 999, overflow: 'hidden', border: 1, borderColor: 'divider',
          bgcolor: alpha(theme.palette.background.paper, 0.7),
          backgroundImage: 'none',
          backdropFilter: 'blur(12px)'
        })}
      >
        <BottomNavigation
          showLabels
          value={current}
          onChange={(_, path: string) => navigate(path)}
          sx={{ bgcolor: 'transparent' }}
        >
          {NAV_ITEMS.map(({ path, label, Icon, AiIcon }) => (
            <BottomNavigationAction
              key={path}
              value={path}
              label={label}
              icon={aiActive && AiIcon ? <AiIcon /> : <Icon />}
              sx={{ minWidth: 0, px: 0.5 }}
            />
          ))}
        </BottomNavigation>
      </Paper>
    </Box>
  )
}
