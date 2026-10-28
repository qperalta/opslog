import { Button, Paper, Stack, Typography } from '@mui/material'
import { useEffect, useState } from 'react'

interface Props {
  endsAt: number
  onAdd: (seconds: number) => void
  onDone: () => void
}

const clock = (seconds: number) => `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`

/* counts down from an end time, so a background tab stays accurate; sits above the bottom nav */
export function RestTimer({ endsAt, onAdd, onDone }: Props) {
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    const id = window.setInterval(() => setNow(Date.now()), 250)
    return () => window.clearInterval(id)
  }, [])

  const left = Math.max(0, Math.ceil((endsAt - now) / 1000))

  useEffect(() => {
    if (left === 0) navigator.vibrate?.(300)
  }, [left])

  return (
    <Paper elevation={6} role="timer" aria-live="polite"
           sx={{ position: 'sticky', bottom: 72, mt: 2, px: 2, py: 1.5, zIndex: 1 }}>
      <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
        <Typography sx={{ flexGrow: 1, fontWeight: 600 }} color={left === 0 ? 'success.main' : 'text.primary'}>
          {left === 0 ? 'Rest done, next set' : `Rest ${clock(left)}`}
        </Typography>
        {left > 0 && <Button size="small" onClick={() => onAdd(30)}>+30 s</Button>}
        <Button size="small" variant={left === 0 ? 'contained' : 'text'} onClick={onDone}>
          {left === 0 ? 'Close' : 'Skip'}
        </Button>
      </Stack>
    </Paper>
  )
}
