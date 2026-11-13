import { Button, Stack, TextField } from '@mui/material'
import { useState } from 'react'
import { useNotify } from '../components/NotifyContext'
import { useSetTiming } from './queries'
import { clock, defaultRest, fixedTarget, parseClock } from './setFormat'
import type { SessionExercise } from './types'

interface Props {
  item: SessionExercise
  onDone: () => void
}

// same limits as ExerciseTimingRequest; blank = the default
function check(text: string, label: string, min: number, max: number) {
  if (text.trim() === '') return { seconds: null, error: null }
  const seconds = parseClock(text)
  if (seconds == null) return { seconds: null, error: `${label} must look like 1:15` }
  if (seconds < min || seconds > max) return { seconds: null, error: `${label} must be ${clock(min)} to ${clock(max)}` }
  return { seconds, error: null }
}

/* the member's own hold and rest times for this exercise, saved for every session */
export function TimingEditor({ item, onDone }: Props) {
  const timing = useSetTiming()
  const notify = useNotify()
  const timed = item.kind === 'TIMED'
  const [hold, setHold] = useState(item.holdSeconds != null ? clock(item.holdSeconds) : '')
  const [rest, setRest] = useState(item.restSeconds != null ? clock(item.restSeconds) : '')
  const [errors, setErrors] = useState<{ hold: string | null, rest: string | null }>({ hold: null, rest: null })

  const onSave = () => {
    const holdCheck = timed ? check(hold, 'Timer', 1, 600) : { seconds: null, error: null }
    const restCheck = check(rest, 'Rest', 0, 900)
    setErrors({ hold: holdCheck.error, rest: restCheck.error })
    if (holdCheck.error || restCheck.error) return
    timing.mutate({ exerciseId: item.exerciseId, body: { holdSeconds: holdCheck.seconds, restSeconds: restCheck.seconds } }, {
      onSuccess: () => {
        notify(`Saved times for ${item.name}`)
        onDone()
      },
      onError: (failure) => notify(failure.message, 'error'),
    })
  }

  const defaultHold = fixedTarget(item)
  return (
    <Stack spacing={1} sx={{ mt: 1.5 }}>
      <Stack direction="row" spacing={1}>
        {timed && (
          <TextField size="small" label="Timer" fullWidth value={hold} onChange={(event) => setHold(event.target.value)}
                     placeholder={defaultHold ? clock(Number(defaultHold)) : 'M:SS'}
                     error={!!errors.hold} helperText={errors.hold ?? 'Blank = the target'}
                     slotProps={{ htmlInput: { inputMode: 'numeric' }, inputLabel: { shrink: true } }} />
        )}
        <TextField size="small" label="Rest" fullWidth value={rest} onChange={(event) => setRest(event.target.value)}
                   placeholder={clock(defaultRest(item))}
                   error={!!errors.rest} helperText={errors.rest ?? `Blank = ${clock(defaultRest(item))}`}
                   slotProps={{ htmlInput: { inputMode: 'numeric' }, inputLabel: { shrink: true } }} />
      </Stack>
      <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end' }}>
        <Button size="small" color="inherit" onClick={onDone}>Cancel</Button>
        <Button size="small" variant="contained" onClick={onSave} disabled={timing.isPending}>Save</Button>
      </Stack>
    </Stack>
  )
}
