import { Box, Stack, Typography } from '@mui/material'
import type { ReactNode } from 'react'

interface PageHeaderProps {
  title: string
  eyebrow?: string
  subtitle?: ReactNode
  action?: ReactNode
  children?: ReactNode
}

export function PageHeader({ title, eyebrow, subtitle, action, children }: PageHeaderProps) {
  return (
    <Box component="header" sx={{ mb: 3 }}>
      <Stack direction="row" spacing={2} sx={{
        alignItems: 'flex-end',
        justifyContent: 'space-between'
      }}>
        <Box sx={{ minWidth: 0 }}>
          {eyebrow && (
            <Typography
              variant="overline"
              color="primary"
              sx={{
                display: 'block',
                fontWeight: 700,
                letterSpacing: '0.08em',
                lineHeight: 1.8
            }}>
              {eyebrow}
            </Typography>
          )}
          <Typography
            variant="h4"
            component="h1"
            sx={{
              fontWeight: 800,
              letterSpacing: '-0.02em',
              lineHeight: 1.15,
              fontSize: { xs: '1.75rem', sm: '2.125rem' }
          }}>
            {title}
          </Typography>
          {subtitle && (
            <Typography
              color="text.secondary"
              sx={{ mt: 0.5
            }}>
              {subtitle}
            </Typography>
          )}
        </Box>
        {action && <Box sx={{ flexShrink: 0 }}>{action}</Box>}
      </Stack>
      {children && <Box sx={{ mt: 2 }}>{children}</Box>}
    </Box>
  )
}
