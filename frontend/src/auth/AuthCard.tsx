import { Box, Paper, Typography } from '@mui/material'
import type { ReactNode } from 'react'
import logo from '../assets/logo.png'

/* centered card shared by the login and register pages */
export function AuthCard({ title, children }: { title: string; children: ReactNode }) {
  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', p: 2, bgcolor: 'background.default' }}>
      <Paper sx={{ p: 3, width: '100%', maxWidth: 400 }}>
        <Box component="img" src={logo} alt="Kaimana Strength Collective" sx={{ display: 'block', width: '100%', maxWidth: 300, mx: 'auto', mb: 1 }} />
        <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', mb: 3 }}>
          OpsLog Training App
        </Typography>
        <Typography variant="h6" component="h1" sx={{ mb: 2 }}>
          {title}
        </Typography>
        {children}
      </Paper>
    </Box>
  )
}
