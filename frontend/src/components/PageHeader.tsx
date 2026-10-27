import { Stack, Typography } from '@mui/material'
import type { ReactNode } from 'react'

export function PageHeader({ title, action }: { title: string; action?: ReactNode }) {
  return (
    <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
      <Typography variant="h5" component="h1">
        {title}
      </Typography>
      {action}
    </Stack>
  )
}
