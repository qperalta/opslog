import CheckIcon from '@mui/icons-material/Check'
import CloseIcon from '@mui/icons-material/Close'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import { Box, IconButton, Stack, TextField, Typography } from '@mui/material'
import { useState } from 'react'
import { z } from 'zod'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { useNotify } from '../components/NotifyContext'
import { useDeleteSet, useSaveSet } from './queries'
import { isCardio, isStrength, measureLabel } from './setFormat'
import type { LoggedSet, SessionExercise } from './types'

const whole = /^\d+$/
const decimal = /^\d{1,4}(\.\d{1,2})?$/

// same limits as LoggedSetRequest
function schemaFor(kind: string) {
  const max = isStrength(kind) ? 100 : isCardio(kind) ? 120 : 7200
  const min = isStrength(kind) ? 0 : 1
  const unit = isStrength(kind) ? 'Reps' : isCardio(kind) ? 'Minutes' : 'Seconds'
  return z.object({
    load: z.string().refine((value) => value === '' || decimal.test(value),
      'Weight must be under 10000 lb with at most 2 decimals'),
    measure: z.string().refine((value) => value !== '', `Enter ${unit.toLowerCase()}`)
      .refine((value) => value === '' || (whole.test(value) && Number(value) >= min && Number(value) <= max),
        `${unit} must be a whole number from ${min} to ${max}`),
  })
}

/* "8-12 reps" -> "8-12", "75 s" -> "75"; only a single number can stand in for an empty field */
const targetText = (item: SessionExercise) => item.target.split(' ')[0]
const fixedTarget = (item: SessionExercise) => whole.test(targetText(item)) ? targetText(item) : ''

// the last logged load, then the target load
const startLoad = (item: SessionExercise) => item.logged.at(-1)?.loadLb ?? item.targetLoadLb

function measureOf(set: LoggedSet, kind: string) {
  if (isStrength(kind)) return String(set.reps)
  return String(isCardio(kind) ? Math.round((set.durationSeconds ?? 0) / 60) : set.durationSeconds)
}

const cell = { flex: 1, minWidth: 0 }

interface Props {
  date: string
  item: SessionExercise
  number: number
  set?: LoggedSet
  active: boolean
  onLogged: () => void
}

/* a logged set, the next set to log, or a set that waits its turn */
export function SetRow({ date, item, number, set, active, onLogged }: Props) {
  const save = useSaveSet()
  const remove = useDeleteSet()
  const notify = useNotify()
  const [load, setLoad] = useState('')
  const [measure, setMeasure] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [confirming, setConfirming] = useState(false)
  const target = { date, position: item.position }
  const placeholderLoad = startLoad(item)

  if (set) {
    const toggleClean = () =>
      save.mutate({
        target: { ...target, setNumber: set.setNumber },
        body: { reps: set.reps, durationSeconds: set.durationSeconds, loadLb: set.loadLb, clean: !set.clean },
      }, { onError: (failure) => notify(failure.message, 'error') })

    const onDelete = () =>
      remove.mutate({ ...target, setNumber: set.setNumber }, {
        onSuccess: () => {
          notify(`Deleted set ${number}`)
          setConfirming(false)
        },
        onError: (failure) => {
          setConfirming(false)
          notify(failure.message, 'error')
        },
      })

    return (
      <>
        <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
          <Typography color="text.secondary" sx={{ width: 20, fontWeight: 600 }}>{number}</Typography>
          <Box sx={{ ...cell, py: 1, px: 1.5, borderRadius: 1, bgcolor: 'action.selected' }}>
            <Typography noWrap>{set.loadLb ?? 'BW'}</Typography>
          </Box>
          <Box sx={{ ...cell, py: 1, px: 1.5, borderRadius: 1, bgcolor: 'action.selected' }}>
            <Typography noWrap>{measureOf(set, item.kind)}</Typography>
          </Box>
          <IconButton aria-label={set.clean ? `Mark set ${number} not clean` : `Mark set ${number} clean`}
                      aria-pressed={!set.clean} color={set.clean ? 'default' : 'warning'}
                      onClick={toggleClean} disabled={save.isPending}>
            <WarningAmberIcon />
          </IconButton>
          <IconButton aria-label={`Delete set ${number}`} onClick={() => setConfirming(true)}>
            <CloseIcon />
          </IconButton>
        </Stack>
        <ConfirmDialog
          open={confirming}
          title={`Delete set ${number}?`}
          message="This removes the set from today's log."
          busy={remove.isPending}
          onConfirm={onDelete}
          onClose={() => setConfirming(false)}
        />
      </>
    )
  }

  const onLog = () => {
    setError(null)
    // an empty field takes the value shown as its placeholder
    const values = {
      load: load.trim() || (placeholderLoad != null ? String(placeholderLoad) : ''),
      measure: measure.trim() || fixedTarget(item),
    }
    const parsed = schemaFor(item.kind).safeParse(values)
    if (!parsed.success) {
      setError(parsed.error.issues[0].message)
      return
    }
    const amount = Number(parsed.data.measure)
    save.mutate({
      target,
      body: {
        reps: isStrength(item.kind) ? amount : null,
        durationSeconds: isStrength(item.kind) ? null : amount * (isCardio(item.kind) ? 60 : 1),
        loadLb: parsed.data.load === '' ? null : Number(parsed.data.load),
        clean: true,
      },
    }, {
      onSuccess: () => {
        notify(`Logged set ${number}`)
        setLoad('')
        setMeasure('')
        onLogged()
      },
      onError: (failure) => setError(failure.message),
    })
  }

  const onClear = () => {
    setLoad('')
    setMeasure('')
    setError(null)
  }

  return (
    <Box>
      <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
        <Typography color={active ? 'text.primary' : 'text.disabled'} sx={{ width: 20, fontWeight: 600 }}>{number}</Typography>
        <TextField size="small" sx={cell} value={load} disabled={!active} onChange={(event) => setLoad(event.target.value)}
                   placeholder={placeholderLoad != null ? String(placeholderLoad) : 'BW'}
                   error={!!error} slotProps={{ htmlInput: { inputMode: 'decimal', 'aria-label': `Set ${number} weight in pounds` } }} />
        <TextField size="small" sx={cell} value={measure} disabled={!active} onChange={(event) => setMeasure(event.target.value)}
                   placeholder={targetText(item)}
                   error={!!error} slotProps={{ htmlInput: { inputMode: 'numeric', 'aria-label': `Set ${number} ${measureLabel(item.kind).toLowerCase()}` } }} />
        <IconButton aria-label={`Clear set ${number}`} onClick={onClear} disabled={!active}>
          <CloseIcon />
        </IconButton>
        <IconButton aria-label={`Log set ${number}`} color="primary" onClick={onLog} disabled={!active || save.isPending}>
          <CheckIcon />
        </IconButton>
      </Stack>
      {error && <Typography variant="caption" color="error" sx={{ display: 'block', pl: 3.5, pt: 0.5 }}>{error}</Typography>}
    </Box>
  )
}
