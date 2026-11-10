import { Box, Paper, Typography } from '@mui/material'
import type { ReactNode } from 'react'

export interface HeaderStat {
  label: string
  value: ReactNode
}

export function HeaderStats({ stats }: { stats: HeaderStat[] }) {
  return (
    <Paper
      variant="outlined"
      sx={{
        display: 'grid',
        gridTemplateColumns: `repeat(${stats.length}, minmax(0, 1fr))`,
        gap: '1px',
        bgcolor: 'divider',
        overflow: 'hidden'
      }}
    >
      {stats.map((stat) => (
        <Box
          key={stat.label}
          sx={{
            bgcolor: 'background.paper',
            px: 2,
            py: 1.5
          }}
        >
          <Typography
            variant="caption"
            color="text.secondary"
            sx={{
              display: 'block',
              fontWeight: 600,
              textTransform: 'uppercase',
              letterSpacing: '0.06em'
            }}
          >
            {stat.label}
          </Typography>
          <Typography
            component="p"
            noWrap
            sx={{
              fontWeight: 700,
              fontSize: '1.05rem',
              mt: 0.25
            }}
          >
            {stat.value}
          </Typography>
        </Box>
      ))}
    </Paper>
  )
}