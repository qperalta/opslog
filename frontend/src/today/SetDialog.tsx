import { zodResolver } from '@hookform/resolvers/zod'
import {
  Alert, Button, Checkbox, Dialog, DialogActions, DialogContent, DialogTitle, FormControlLabel, Stack, TextField,
} from '@mui/material'
import { useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { z } from 'zod'
import { applyServerErrors } from '../api/formErrors'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { useNotify } from '../components/NotifyContext'
import { useDeleteSet, useSaveSet } from './queries'
import { isCardio, isStrength } from './setFormat'
import type { LoggedSet, SessionExercise } from './types'

const whole = /^\d+$/
const load = /^\d{1,4}(\.\d{1,2})?$/

// same limits as LoggedSetRequest; text fields so an empty load stays empty
function schemaFor(kind: string) {
  const strength = isStrength(kind)
  const maxDuration = isCardio(kind) ? 120 : 7200
  const unit = isCardio(kind) ? 'Minutes' : 'Seconds'
  return z.object({
    reps: z.string().trim(),
    duration: z.string().trim(),
    loadLb: z.string().trim().refine((value) => value === '' || load.test(value),
      'Load must be under 10000 lb with at most 2 decimals'),
    clean: z.boolean(),
  }).superRefine((values, ctx) => {
    if (strength) {
      if (!whole.test(values.reps) || Number(values.reps) > 100) {
        ctx.addIssue({ code: 'custom', path: ['reps'], message: 'Reps must be a whole number from 0 to 100' })
      }
    } else if (!whole.test(values.duration) || Number(values.duration) < 1 || Number(values.duration) > maxDuration) {
      ctx.addIssue({ code: 'custom', path: ['duration'], message: `${unit} must be a whole number from 1 to ${maxDuration}` })
    }
  })
}

type SetValues = z.infer<ReturnType<typeof schemaFor>>

interface Props {
  date: string
  item: SessionExercise
  set?: LoggedSet
  onLogged: () => void
  onClose: () => void
}

/* mount with a key so it starts fresh for each set */
export function SetDialog({ date, item, set, onLogged, onClose }: Props) {
  const save = useSaveSet()
  const remove = useDeleteSet()
  const notify = useNotify()
  const [formError, setFormError] = useState<string | null>(null)
  const [confirming, setConfirming] = useState(false)
  const strength = isStrength(item.kind)
  const cardio = isCardio(item.kind)

  // a new set starts from the last one, then the target load
  const last = item.logged.at(-1)
  const start = set ?? last
  const startLoad = start ? start.loadLb : item.targetLoadLb
  const startSeconds = start?.durationSeconds
  const { control, register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm<SetValues>({
    resolver: zodResolver(schemaFor(item.kind)),
    defaultValues: {
      reps: start?.reps != null ? String(start.reps) : '',
      duration: startSeconds != null ? String(cardio ? Math.round(startSeconds / 60) : startSeconds) : '',
      loadLb: startLoad != null ? String(startLoad) : '',
      clean: set?.clean ?? true,
    },
  })

  const target = { date, position: item.position, setNumber: set?.setNumber }

  const onSubmit = async (values: SetValues) => {
    setFormError(null)
    const duration = values.duration === '' ? null : Number(values.duration) * (cardio ? 60 : 1)
    try {
      const saved = await save.mutateAsync({
        target,
        body: {
          reps: strength ? Number(values.reps) : null,
          durationSeconds: strength ? null : duration,
          loadLb: values.loadLb === '' ? null : Number(values.loadLb),
          clean: values.clean,
        },
      })
      notify(set ? `Updated set ${saved.setNumber}` : `Logged set ${saved.setNumber}`)
      if (!set) onLogged()
      onClose()
    } catch (error) {
      const message = applyServerErrors(error, setError, ['reps', 'loadLb'])
      setFormError(message)
    }
  }

  const onDelete = () =>
    remove.mutate(target, {
      onSuccess: () => {
        notify(`Deleted set ${set?.setNumber}`)
        onClose()
      },
      onError: (error) => {
        setConfirming(false)
        setFormError(error.message)
      },
    })

  return (
    <>
      <Dialog open onClose={onClose} fullWidth maxWidth="xs">
        <DialogTitle>{set ? `Edit set ${set.setNumber}` : 'Log a set'} · {item.name}</DialogTitle>
        <DialogContent>
          <Stack component="form" id="set-form" spacing={2} sx={{ pt: 1 }} noValidate onSubmit={handleSubmit(onSubmit)}>
            {formError && <Alert severity="error">{formError}</Alert>}
            {strength ? (
              <TextField label="Reps" autoFocus slotProps={{ htmlInput: { inputMode: 'numeric' } }} {...register('reps')}
                         error={!!errors.reps} helperText={errors.reps?.message ?? `Target ${item.target}`} />
            ) : (
              <TextField label={cardio ? 'Minutes' : 'Seconds'} autoFocus slotProps={{ htmlInput: { inputMode: 'numeric' } }}
                         {...register('duration')} error={!!errors.duration}
                         helperText={errors.duration?.message ?? `Target ${item.target}`} />
            )}
            <TextField label="Load (lb)" slotProps={{ htmlInput: { inputMode: 'decimal' } }} {...register('loadLb')}
                       error={!!errors.loadLb} helperText={errors.loadLb?.message ?? 'Leave empty for bodyweight'} />
            <Controller
              name="clean"
              control={control}
              render={({ field }) => (
                <FormControlLabel
                  control={<Checkbox checked={field.value} onChange={(event) => field.onChange(event.target.checked)} />}
                  label="Clean technique"
                />
              )}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          {set && <Button color="error" onClick={() => setConfirming(true)} sx={{ mr: 'auto' }}>Delete</Button>}
          <Button onClick={onClose}>Cancel</Button>
          <Button type="submit" form="set-form" variant="contained" disabled={isSubmitting}>
            {set ? 'Save' : 'Log set'}
          </Button>
        </DialogActions>
      </Dialog>
      <ConfirmDialog
        open={confirming}
        title={`Delete set ${set?.setNumber}?`}
        message="This removes the set from today's log."
        busy={remove.isPending}
        onConfirm={onDelete}
        onClose={() => setConfirming(false)}
      />
    </>
  )
}
