import StopIcon from '@mui/icons-material/Stop'
import TimerIcon from '@mui/icons-material/Timer'
import { Button } from '@mui/material'
import { useEffect, useState } from 'react'
import { clock } from './setFormat'
import { useStoredTimer } from './useStoredTimer'

interface Props {
  date: string
  storageKey: string
  seconds: number
  onDone: (seconds: number) => void
}

/* counts down a hold from a stored end time; stopping early reports the seconds held */
export function HoldTimer({ date, storageKey, seconds, onDone }: Props) {
  const [endsAt, setEndsAt] = useStoredTimer(storageKey, date)
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    if (endsAt == null) return
    const id = window.setInterval(() => {
      const time = Date.now()
      if (time >= endsAt) {
        window.clearInterval(id)
        navigator.vibrate?.(300)
        setEndsAt(null)
        onDone(seconds)
      } else {
        setNow(time)
      }
    }, 250)
    return () => window.clearInterval(id)
  }, [endsAt, seconds, onDone, setEndsAt])

  if (endsAt == null) {
    return (
      <Button size="small" variant="outlined" startIcon={<TimerIcon />} onClick={() => {
        setNow(Date.now())
        setEndsAt(Date.now() + seconds * 1000)
      }}>
        Start {clock(seconds)}
      </Button>
    )
  }

  const left = Math.max(0, Math.ceil((endsAt - now) / 1000))
  return (
    <Button size="small" variant="contained" startIcon={<StopIcon />} aria-live="polite" onClick={() => {
      setEndsAt(null)
      onDone(Math.max(1, seconds - left))
    }}>
      {clock(left)}
    </Button>
  )
}
