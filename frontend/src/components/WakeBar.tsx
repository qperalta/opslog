import { useSlow } from './useSlow'
import { useIsFetching, useIsMutating } from '@tanstack/react-query'
import { Box, LinearProgress, Typography } from '@mui/material'

export function WakeBar() {
  const fetching = useIsFetching()
  const mutating = useIsMutating()
  const slow = useSlow(fetching + mutating > 0)

  if(!slow) return null
  return (
    <Box role="status" sx={{ px: 2, pb: 1 }}>
      <Typography variant="caption" sx={{ display: 'block', textAlign: 'center' }}>
        The server may be waking up. This can take up to a minute.
      </Typography>
      <LinearProgress color="inherit" sx={{ mt: 0.5 }} />
    </Box>
  )
}