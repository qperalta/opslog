import InsightsIcon from '@mui/icons-material/Insights'
import {
  Avatar, Badge, Box, Divider, IconButton, ListItemIcon, ListItemText, Menu, MenuItem, Tooltip, Typography,
} from '@mui/material'
import { useState } from 'react'
import { useNavigate } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { initialsOf, timeAgo } from '../components/format'
import { useMarkNotificationsRead, useNotifications } from './queries'

/* the member's initials with an unread badge; opens what's new and a link to Stats */
export function AvatarMenu() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const notifications = useNotifications()
  const markRead = useMarkNotificationsRead()
  const [anchor, setAnchor] = useState<HTMLElement | null>(null)
  const unread = notifications.data?.unreadCount ?? 0
  const items = notifications.data?.items ?? []

  // closing the menu counts as having seen them, so the New dots stay while it's open
  const close = () => {
    setAnchor(null)
    if (unread > 0) markRead.mutate()
  }

  return (
    <>
      <Tooltip title={user?.displayName ?? 'Notifications'}>
        <IconButton color="inherit" onClick={(event) => setAnchor(event.currentTarget)} sx={{ p: 0.5 }}
                    aria-label={unread > 0 ? `${unread} new notifications` : 'Notifications'}>
          <Badge color="error" badgeContent={unread} overlap="circular">
            <Avatar sx={{ width: 34, height: 34, fontSize: '0.875rem', fontWeight: 600, color: 'inherit',
              bgcolor: 'rgba(255,255,255,0.2)', border: '1.5px solid rgba(255,255,255,0.7)' }}>
              {initialsOf(user?.displayName)}
            </Avatar>
          </Badge>
        </IconButton>
      </Tooltip>
      <Menu anchorEl={anchor} open={anchor != null} onClose={close}
            slotProps={{ paper: { sx: { width: 320, maxWidth: 'calc(100vw - 32px)' } } }}>
        <Box sx={{ px: 2, py: 1 }}>
          <Typography variant="overline" color="text.secondary" sx={{ lineHeight: 1 }}>What's new</Typography>
        </Box>
        {notifications.isError && (
          <Typography variant="body2" color="error" sx={{ px: 2, pb: 1 }}>{notifications.error.message}</Typography>
        )}
        {notifications.isSuccess && items.length === 0 && (
          <Typography variant="body2" color="text.secondary" sx={{ px: 2, pb: 1 }}>Nothing new yet.</Typography>
        )}
        {items.map((item) => (
          <Box key={item.id} sx={{ px: 2, py: 1 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              {item.unread && <Box aria-label="New" sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: 'error.main', flexShrink: 0 }} />}
              <Typography variant="body2" sx={{ fontWeight: 600 }}>{item.title}</Typography>
            </Box>
            <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'normal' }}>{item.body}</Typography>
            <Typography variant="caption" color="text.secondary">{timeAgo(item.publishedAt)}</Typography>
          </Box>
        ))}
        <Divider />
        <MenuItem onClick={() => {
          close()
          navigate('/stats')
        }}>
          <ListItemIcon><InsightsIcon fontSize="small" /></ListItemIcon>
          <ListItemText>Your stats</ListItemText>
        </MenuItem>
      </Menu>
    </>
  )
}
