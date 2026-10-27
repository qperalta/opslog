import { Box, Paper, Typography } from '@mui/material'
import type { ReactNode } from 'react'

/* centered card shared by the login and register pages */
export function AuthCard({ title, children }: { title: string; children: ReactNode }) {
  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', p: 2, bgcolor: 'background.default' }}>
      <Paper sx={{ p: 3, width: '100%', maxWidth: 400 }}>
        <Typography variant="h4" component="div" sx={{ fontWeight: 700 }}>
          OpsLog
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Kaimana Strength Collective
        </Typography>
        <Typography variant="h6" component="h1" sx={{ mb: 2 }}>
          {title}
        </Typography>
        {children}
      </Paper>
    </Box>
  )
}
